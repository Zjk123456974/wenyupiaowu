<template>
  <div class="venue3d">
    <div ref="canvasWrap" class="venue3d-canvas"></div>

    <div class="venue3d-bar">
      <el-radio-group v-model="mode" size="small">
        <el-radio-button label="pano">全景漫游</el-radio-button>
        <el-radio-button label="seat" :disabled="!focusCode">从座位看</el-radio-button>
      </el-radio-group>
      <span class="venue3d-venue">{{ venueName }}<em class="venue3d-ver">v3</em></span>
      <span class="venue3d-hint">
        <template v-if="mode === 'seat'">当前视角：{{ focusLabel }}<template v-if="focusSoldHint">（参考视角）</template></template>
        <template v-else>拖拽旋转 · 滚轮缩放 · 点击蓝色座位即可切换到该座位视野</template>
      </span>
    </div>

    <div class="venue3d-legend">
      <span><i class="dot dot-avail"></i>可选（区域内按票档配色）</span>
      <span><i class="dot dot-sold"></i>已售（含观众）</span>
      <span><i class="dot dot-sel"></i>已选/聚焦</span>
    </div>
  </div>
</template>

<script setup>
/**
 * Venue3DViewer —— 选座 3D 场馆视野预览 v2（多场馆类型 + 高真实感 + 座位第一人称）
 * 支持四种场馆模型：
 *   - arena_bowl 碗形综合馆（演唱会 / 电竞综合，中心舞台环绕看台）
 *   - theatre     镜框式剧院（舞台在一端，正面观众席阶梯排布）
 *   - stadium     标准体育场（中心草坪 + 环形看台）
 *   - concert     露天音乐场（夜空 + 中心舞台，开放顶）
 * 核心体验：
 *   1) 父组件选中座位后自动"飞入"该座位第一人称视角（FOV 68 接近人眼），镜头平滑过渡；
 *   2) 已售座位渲染为灰色 + 前排观众头部剪影（拟真上座效果），点击已售座位提示不可选；
 *   3) 分区名称/票价浮动标签，直观对应"我在场馆的哪个位置"；
 *   4) LED 屏氛围变色；未开启在线选座（座位由系统安排）的活动提供默认场馆布局预览。
 */
import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from 'vue'
import * as THREE from '@/libs/three.module.js'
import { ElMessage } from 'element-plus'

const props = defineProps({
  // 解析后的座位分区：[{ id, name, rows, cols, price }]；为空时生成默认布局（系统安排座位场次）
  sections: { type: Array, default: () => [] },
  // 父组件已选座位编码，如 "A-3-5"
  selectedSeatCode: { type: String, default: '' },
  // 已售座位编码数组
  occupied: { type: Array, default: () => [] },
  // 场馆模型：arena_bowl / theatre / stadium / concert
  model: { type: String, default: 'arena_bowl' }
})

const venueName = computed(() => (PRESETS[props.model] || PRESETS.arena_bowl).name)

// ---- 场馆预设 ----
const PRESETS = {
  arena_bowl: {
    name: '碗形综合馆',
    sky: 'indoor', roof: 'dome', panoRadius: 44,
    stageLookAt: [0, 2.6, 0], stage: 'center',
    seat: { type: 'ring', r0: 10, rowDepth: 1.0, tierH: 0.7, rx: 1, rz: 1, baseY: 0.6, segs: 48, tierGap: 4 }
  },
  theatre: {
    name: '镜框式剧院',
    sky: 'indoor', roof: 'ceiling', panoRadius: 36,
    stageLookAt: [0, 3.2, -15], stage: 'theatre',
    seat: { type: 'rect', width: 30, rowDepth: 1.05, tierH: 0.5, baseY: 0.7, stageFrontZ: -12 }
  },
  stadium: {
    name: '标准体育场',
    sky: 'open', roof: 'ring', panoRadius: 58,
    stageLookAt: [0, 1.6, 0], stage: 'pitch',
    seat: { type: 'ring', r0: 17, rowDepth: 1.15, tierH: 0.85, rx: 1.35, rz: 1.0, baseY: 0.8, segs: 64, tierGap: 4 }
  },
  concert: {
    name: '露天音乐场',
    sky: 'night', roof: 'none', panoRadius: 50,
    stageLookAt: [0, 2.6, 0], stage: 'center',
    seat: { type: 'ring', r0: 11, rowDepth: 1.05, tierH: 0.7, rx: 1, rz: 1, baseY: 0.6, segs: 48, tierGap: 4 }
  }
}
const getPreset = (m) => PRESETS[m] || PRESETS.arena_bowl

const canvasWrap = ref(null)
const mode = ref('pano')
const focusCode = ref('')
const focusLabel = ref('')
const focusSoldHint = ref(false)

// ---- Three 基础对象 ----
let renderer, scene, camera, raf
let seatBaseMesh = null, seatBackMesh = null, crowdMesh = null
let seatBaseGeo = null, seatBackGeo = null, crowdGeo = null, crowdMat = null, seatMat = null
let codeToIndex = new Map()
let seatMeta = [] // { code, x, y, z, ang, radius }
const occupiedSet = new Set()
let venueOuterR = 40
let venueTopY = 10
let sectionSprites = []
const screenMats = [] // LED 材质（动效）

// 全景相机球坐标
const pano = { radius: 44, theta: Math.PI / 2.6, phi: 0.95, target: new THREE.Vector3(0, 4, 0) }

// 镜头飞行过渡状态
let camTween = null
const camLook = new THREE.Vector3(0, 4, 0)

const COLOR = {
  avail: new THREE.Color(0x4a90d9),
  sold: new THREE.Color(0x595f6b),
  sel: new THREE.Color(0xffcc33)
}
// 票档分层座椅配色（0=最贵内圈 → 外圈看台），真实场馆按区域分色
const TIER_COLORS = [0xc0483f, 0x4a90d9, 0x4f9f8a, 0x8a6bbf, 0xb08b3a]
const tierColor = (t) => TIER_COLORS[(t || 0) % TIER_COLORS.length]
// 观众剪影配色（深色系，模拟人群）
const CROWD_COLORS = [0x2b2f38, 0x3a2f2c, 0x24303c, 0x322a34, 0x2c3630]

function parseCode(code) {
  const p = String(code).split('-')
  if (p.length < 3) return null
  return { secId: p[0], row: Number(p[1]), col: Number(p[2]) }
}

// 未开启在线选座（座位由系统安排）时的默认场馆布局：模拟 6 个分区
function defaultSections() {
  return ['A', 'B', 'C', 'D', 'E', 'F'].map((id, i) => ({
    id, name: `${id} 区`, rows: 8, cols: 12, price: 0
  }))
}

// 座位 → 3D 坐标（按场馆类型）
// tierIdx：票档层级（0=最贵最靠近舞台最低，越大越远越高），实现"看台高位俯视"的真实分层
function seatPosition(secIdx, totalSec, row, rows, col, cols, seat, tierIdx = 0) {
  if (seat.type === 'rect') {
    // 剧院式：分区左右切片，排沿纵深（远离舞台），逐排抬高
    const sliceW = seat.width / totalSec
    const secX0 = -seat.width / 2 + secIdx * sliceW
    const x = secX0 + ((col - 0.5) / cols) * sliceW
    const z = seat.stageFrontZ + (row - 1) * seat.rowDepth
    const y = seat.baseY + (row - 1) * seat.tierH
    return { x, y, z, ang: Math.PI, radius: Math.abs(x) }
  }
  // 环形（碗形 / 体育场 / 露天）：分区绕中心均匀切分扇区，按票档分层（内圈贵、外圈看台）
  const tierOff = tierIdx * (seat.tierGap || 4)
  const angStep = (Math.PI * 2) / totalSec
  const secCenter = secIdx * angStep
  const colFrac = col - (cols + 1) / 2
  const colAng = colFrac * ((angStep * 0.82) / cols)
  const ang = secCenter + colAng
  const radius = seat.r0 + tierOff * seat.rowDepth + (row - 1) * seat.rowDepth
  const y = seat.baseY + tierOff * seat.tierH + (row - 1) * seat.tierH
  const x = radius * seat.rx * Math.cos(ang)
  const z = radius * seat.rz * Math.sin(ang)
  return { x, y, z, ang, radius }
}

// ---- 天空 / 背景 ----
function makeGradient(top, bottom) {
  const c = document.createElement('canvas'); c.width = 4; c.height = 256
  const ctx = c.getContext('2d')
  const g = ctx.createLinearGradient(0, 0, 0, 256)
  g.addColorStop(0, top); g.addColorStop(1, bottom)
  ctx.fillStyle = g; ctx.fillRect(0, 0, 4, 256)
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}
function addStars() {
  const g = new THREE.BufferGeometry()
  const n = 700
  const pos = new Float32Array(n * 3)
  for (let i = 0; i < n; i++) {
    const r = 220 + Math.random() * 140
    const th = Math.random() * Math.PI * 2
    const ph = Math.random() * Math.PI * 0.5
    pos[i * 3] = r * Math.sin(ph) * Math.cos(th)
    pos[i * 3 + 1] = r * Math.cos(ph) + 20
    pos[i * 3 + 2] = r * Math.sin(ph) * Math.sin(th)
  }
  g.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  const m = new THREE.PointsMaterial({ color: 0xffffff, size: 1.3, sizeAttenuation: true, transparent: true, opacity: 0.9 })
  scene.add(new THREE.Points(g, m))
}
function applySky(sky) {
  if (sky === 'indoor') {
    scene.background = new THREE.Color(0x0a0f1c)
    scene.fog = new THREE.Fog(0x0a0f1c, 70, 170)
  } else if (sky === 'open') {
    scene.background = makeGradient('#9ec9ff', '#e8f3ff')
    scene.fog = new THREE.Fog(0xcfe4ff, 130, 340)
  } else { // night
    scene.background = makeGradient('#05060f', '#0f1b3a')
    scene.fog = new THREE.Fog(0x070b18, 100, 260)
    addStars()
  }
}

// ---- 场馆装饰 ----
function buildCenterStage(preset) {
  const isConcert = preset.sky === 'night'
  const R = isConcert ? 7 : 6
  const stage = new THREE.Mesh(
    new THREE.CylinderGeometry(R, R, 1.4, 48),
    new THREE.MeshStandardMaterial({ color: 0x1c2540, roughness: 0.5, metalness: 0.3 })
  )
  stage.position.y = 0.7
  stage.castShadow = true; stage.receiveShadow = true
  scene.add(stage)
  // 发光台面
  const top = new THREE.Mesh(
    new THREE.CylinderGeometry(R - 0.4, R - 0.4, 0.12, 48),
    new THREE.MeshStandardMaterial({ color: 0x2e6cff, emissive: 0x2e6cff, emissiveIntensity: 0.9, roughness: 0.3 })
  )
  top.position.y = 1.45
  scene.add(top)
  // 背景 LED 视频墙（演出画面纹理）
  const wallMat = new THREE.MeshStandardMaterial({
    color: 0xffffff, map: getScreenTexture(), emissiveMap: getScreenTexture(),
    emissive: 0x88bbff, emissiveIntensity: 0.75, side: THREE.DoubleSide, roughness: 0.4
  })
  screenMats.push(wallMat)
  const wall = new THREE.Mesh(new THREE.PlaneGeometry(R * 2.4, 7), wallMat)
  wall.position.set(0, 6.5, -R - 2)
  scene.add(wall)
  // 四块环绕侧屏
  for (let i = 0; i < 4; i++) {
    const a = (Math.PI / 2) * i
    const sMat = wallMat.clone()
    screenMats.push(sMat)
    const s = new THREE.Mesh(new THREE.PlaneGeometry(7, 4.2), sMat)
    s.position.set((R + 3) * Math.cos(a), 6, (R + 3) * Math.sin(a))
    s.lookAt(0, 6, 0)
    scene.add(s)
  }
  // 桁架灯点
  const trussMat = new THREE.MeshStandardMaterial({ color: 0x333a4d, metalness: 0.6, roughness: 0.4 })
  for (let i = 0; i < 8; i++) {
    const a = (Math.PI * 2 / 8) * i
    const bar = new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.4, 0.4), trussMat)
    bar.position.set((R + 1) * Math.cos(a), 11, (R + 1) * Math.sin(a))
    scene.add(bar)
  }
  // 舞台光束（上宽下窄射向舞台，体积光质感）
  const beamMat = new THREE.MeshBasicMaterial({
    color: 0x7fb3ff, transparent: true, opacity: 0.13,
    blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
  })
  for (let i = 0; i < 8; i++) {
    const a = (Math.PI * 2 / 8) * i
    const pTop = new THREE.Vector3((R + 2.5) * Math.cos(a), 14, (R + 2.5) * Math.sin(a))
    const pBot = new THREE.Vector3(pTop.x * 0.22, 0.6, pTop.z * 0.22)
    scene.add(beamBetween(pTop, pBot, 0.32, 1.7, beamMat))
  }
}

function buildTheatreStage(preset) {
  const z = preset.seat.stageFrontZ - 2
  // 舞台台体
  const stage = new THREE.Mesh(
    new THREE.BoxGeometry(24, 2.2, 10),
    new THREE.MeshStandardMaterial({ color: 0x2a2f3a, roughness: 0.6, metalness: 0.2 })
  )
  stage.position.set(0, 1.1, z)
  stage.castShadow = true; stage.receiveShadow = true
  scene.add(stage)
  // 台口发光边
  const lip = new THREE.Mesh(
    new THREE.BoxGeometry(24.2, 0.15, 10.2),
    new THREE.MeshStandardMaterial({ color: 0xffd27a, emissive: 0xffb347, emissiveIntensity: 0.8 })
  )
  lip.position.set(0, 2.25, z)
  scene.add(lip)
  // 天幕大屏（演出画面纹理）
  const cycMat = new THREE.MeshStandardMaterial({
    color: 0xffffff, map: getScreenTexture(), emissiveMap: getScreenTexture(),
    emissive: 0x88bbff, emissiveIntensity: 0.6, side: THREE.DoubleSide, roughness: 0.5
  })
  screenMats.push(cycMat)
  const cyc = new THREE.Mesh(new THREE.PlaneGeometry(24, 12), cycMat)
  cyc.position.set(0, 7, z - 5.2)
  scene.add(cyc)
  // 镜框式台口上沿
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x6b4a2b, roughness: 0.7 })
  const top = new THREE.Mesh(new THREE.BoxGeometry(26, 1.6, 1), frameMat)
  top.position.set(0, 15, z + 5.2)
  scene.add(top)
  // 两侧红幕
  const curtainMat = new THREE.MeshStandardMaterial({ color: 0x8a1f2b, roughness: 0.9 })
  for (const sgn of [-1, 1]) {
    const cur = new THREE.Mesh(new THREE.BoxGeometry(4, 14, 0.6), curtainMat)
    cur.position.set(sgn * 13, 8, z + 5.0)
    scene.add(cur)
  }
  // 脚灯
  const footMat = new THREE.MeshStandardMaterial({ color: 0xffe0a0, emissive: 0xffcf6e, emissiveIntensity: 1 })
  for (let i = -10; i <= 10; i++) {
    const f = new THREE.Mesh(new THREE.SphereGeometry(0.18, 8, 8), footMat)
    f.position.set(i * 1.0, 2.3, z + 5.2)
    scene.add(f)
  }
}

// LED 大屏"演出画面"纹理：渐变底 + 彩色光束 + 光点，替代纯色假屏
let screenTexture = null
function getScreenTexture() {
  if (screenTexture) return screenTexture
  const c = document.createElement('canvas')
  c.width = 512; c.height = 256
  const ctx = c.getContext('2d')
  const g = ctx.createLinearGradient(0, 0, 0, 256)
  g.addColorStop(0, '#0b1e4d'); g.addColorStop(1, '#123a7a')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 512, 256)
  const colors = ['#4da3ff', '#8a6cff', '#41e0c0', '#ff6c9f']
  ctx.globalAlpha = 0.35
  for (let i = 0; i < 7; i++) {
    ctx.fillStyle = colors[i % colors.length]
    const x = 40 + i * 68
    ctx.beginPath()
    ctx.moveTo(x, 256); ctx.lineTo(x + 18, 80); ctx.lineTo(x + 46, 80); ctx.lineTo(x + 32, 256)
    ctx.closePath(); ctx.fill()
  }
  ctx.globalAlpha = 0.9
  for (let i = 0; i < 70; i++) {
    ctx.fillStyle = '#bfe0ff'
    ctx.beginPath()
    ctx.arc(Math.random() * 512, Math.random() * 200, Math.random() * 2.4 + 0.6, 0, 7)
    ctx.fill()
  }
  screenTexture = new THREE.CanvasTexture(c)
  screenTexture.colorSpace = THREE.SRGBColorSpace
  return screenTexture
}

function makeGrassTexture() {
  const c = document.createElement('canvas'); c.width = 128; c.height = 128
  const ctx = c.getContext('2d')
  for (let i = 0; i < 128; i += 16) {
    ctx.fillStyle = (i / 16) % 2 ? '#2a8335' : '#338f3e'
    ctx.fillRect(0, i, 128, 16)
  }
  const t = new THREE.CanvasTexture(c)
  t.wrapS = t.wrapT = THREE.RepeatWrapping
  t.repeat.set(6, 9)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

function buildPitch(preset) {
  // 跑道 / 内场（红）
  const infill = new THREE.Mesh(
    new THREE.PlaneGeometry(40, 54),
    new THREE.MeshStandardMaterial({ color: 0xb24a2e, roughness: 1 })
  )
  infill.rotation.x = -Math.PI / 2
  infill.position.y = 0.01
  infill.receiveShadow = true
  scene.add(infill)
  // 草皮（剪草条纹）
  const grass = new THREE.Mesh(
    new THREE.PlaneGeometry(30, 46),
    new THREE.MeshStandardMaterial({ map: makeGrassTexture(), roughness: 1 })
  )
  grass.rotation.x = -Math.PI / 2
  grass.position.y = 0.02
  grass.receiveShadow = true
  scene.add(grass)
  // 中圈
  const circle = new THREE.Mesh(
    new THREE.RingGeometry(4.4, 4.6, 48),
    new THREE.MeshBasicMaterial({ color: 0xffffff, side: THREE.DoubleSide })
  )
  circle.rotation.x = -Math.PI / 2
  circle.position.y = 0.03
  scene.add(circle)
  // 记分牌大屏（一端，演出画面纹理）
  const boardMat = new THREE.MeshStandardMaterial({
    color: 0xffffff, map: getScreenTexture(), emissiveMap: getScreenTexture(),
    emissive: 0x88bbff, emissiveIntensity: 0.65, side: THREE.DoubleSide, roughness: 0.4
  })
  screenMats.push(boardMat)
  const board = new THREE.Mesh(new THREE.PlaneGeometry(16, 8), boardMat)
  board.position.set(0, 9, -28)
  scene.add(board)
}

// 在 p1、p2 之间建一根圆杆（桁架梁 / 光束锥通用）
function beamBetween(p1, p2, rTop, rBottom, mat) {
  const dir = new THREE.Vector3().subVectors(p2, p1)
  const len = dir.length()
  const geo = new THREE.CylinderGeometry(rTop, rBottom, len, 10, 1, true)
  const mesh = new THREE.Mesh(geo, mat)
  mesh.position.copy(p1).addScaledVector(dir, 0.5)
  mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), dir.clone().normalize())
  return mesh
}

function buildRoof(preset) {
  const outer = venueOuterR + 8
  if (preset.roof === 'dome') {
    // 场馆外墙（内壁可见），替代突兀的半透明穹顶
    const wall = new THREE.Mesh(
      new THREE.CylinderGeometry(outer, outer, 20, 64, 1, true),
      new THREE.MeshStandardMaterial({ color: 0x1c2434, side: THREE.BackSide, roughness: 0.95 })
    )
    wall.position.y = 10
    scene.add(wall)
    // 桁架屋盖：外圈环梁 + 内圈环梁 + 放射梁
    const beamMat = new THREE.MeshStandardMaterial({ color: 0x39465e, roughness: 0.7, metalness: 0.35 })
    const outerR = outer - 1
    const innerR = outerR * 0.5
    const ring1 = new THREE.Mesh(new THREE.TorusGeometry(outerR, 0.55, 8, 72), beamMat)
    ring1.rotation.x = Math.PI / 2
    ring1.position.y = 18
    scene.add(ring1)
    const ring2 = new THREE.Mesh(new THREE.TorusGeometry(innerR, 0.45, 8, 64), beamMat)
    ring2.rotation.x = Math.PI / 2
    ring2.position.y = 23
    scene.add(ring2)
    for (let i = 0; i < 20; i++) {
      const a = (Math.PI * 2 / 20) * i
      scene.add(beamBetween(
        new THREE.Vector3(outerR * Math.cos(a), 18, outerR * Math.sin(a)),
        new THREE.Vector3(innerR * Math.cos(a), 23, innerR * Math.sin(a)),
        0.22, 0.22, beamMat
      ))
    }
  } else if (preset.roof === 'ceiling') {
    const ceil = new THREE.Mesh(
      new THREE.PlaneGeometry(80, 80),
      new THREE.MeshStandardMaterial({ color: 0x10141f, side: THREE.DoubleSide, roughness: 1 })
    )
    ceil.rotation.x = Math.PI / 2
    ceil.position.y = 22
    scene.add(ceil)
  } else if (preset.roof === 'ring') {
    const r = venueOuterR + 4
    const roof = new THREE.Mesh(
      new THREE.TorusGeometry(r, 2.2, 12, 80),
      new THREE.MeshStandardMaterial({ color: 0x2c3344, roughness: 0.8, metalness: 0.3 })
    )
    roof.rotation.x = Math.PI / 2
    roof.position.y = venueTopY + 6
    scene.add(roof)
  }
  // none → 露天不建顶
}

function buildDecor(preset) {
  if (preset.stage === 'center') buildCenterStage(preset)
  else if (preset.stage === 'theatre') buildTheatreStage(preset)
  else if (preset.stage === 'pitch') buildPitch(preset)
  buildRoof(preset)
}

// ---- 分区浮动标签（Sprite 文字：分区名 + 票价） ----
function makeSectionSprite(text) {
  const c = document.createElement('canvas')
  c.width = 256; c.height = 72
  const ctx = c.getContext('2d')
  ctx.fillStyle = 'rgba(12, 18, 32, 0.78)'
  const r = 16
  ctx.beginPath()
  ctx.moveTo(r, 0); ctx.lineTo(256 - r, 0); ctx.quadraticCurveTo(256, 0, 256, r)
  ctx.lineTo(256, 72 - r); ctx.quadraticCurveTo(256, 72, 256 - r, 72)
  ctx.lineTo(r, 72); ctx.quadraticCurveTo(0, 72, 0, 72 - r)
  ctx.lineTo(0, r); ctx.quadraticCurveTo(0, 0, r, 0)
  ctx.fill()
  ctx.font = 'bold 34px "Microsoft YaHei", sans-serif'
  ctx.fillStyle = '#8ec9ff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(text, 128, 38)
  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: false })
  const sp = new THREE.Sprite(mat)
  sp.scale.set(5.4, 1.5, 1)
  return sp
}

function buildSectionLabels(sections, seatCfg, preset, totalSec, tierOfSec) {
  // 清理旧标签
  sectionSprites.forEach(sp => {
    scene.remove(sp)
    if (sp.material) {
      if (sp.material.map) sp.material.map.dispose()
      sp.material.dispose()
    }
  })
  sectionSprites = []
  if (!sections.length) return
  sections.forEach((sec, secIdx) => {
    const rows = Number(sec.rows) || 0
    const cols = Number(sec.cols) || 0
    if (!rows || !cols) return
    // 取分区中部座位的坐标作为标签锚点，向上抬升
    const midRow = Math.ceil(rows / 2) + 2
    const midCol = Math.ceil(cols / 2)
    const p = seatPosition(secIdx, totalSec, midRow, rows, midCol, cols, seatCfg, tierOfSec ? (tierOfSec.get(secIdx) || 0) : 0)
    const label = sec.price ? `${sec.name} ¥${Number(sec.price)}` : sec.name
    const sp = makeSectionSprite(label)
    sp.position.set(p.x, p.y + 3.2, p.z)
    scene.add(sp)
    sectionSprites.push(sp)
  })
}

// ---- 观众剪影：已售座位放置低模头部，拟真上座效果 ----
function buildCrowd() {
  if (crowdMesh) { scene.remove(crowdMesh); crowdMesh = null }
  if (crowdGeo) crowdGeo.dispose()
  if (crowdMat) crowdMat.dispose()
  const soldSeats = seatMeta.filter(m => occupiedSet.has(m.code))
  if (!soldSeats.length) return
  crowdGeo = new THREE.SphereGeometry(0.16, 10, 8)
  crowdMat = new THREE.MeshStandardMaterial({ roughness: 0.95 })
  crowdMesh = new THREE.InstancedMesh(crowdGeo, crowdMat, soldSeats.length)
  const dummy = new THREE.Object3D()
  const col = new THREE.Color()
  const preset = getPreset(props.model)
  const isTheatre = preset.seat.type === 'rect'
  soldSeats.forEach((m, i) => {
    // 头部略朝舞台方向偏移（坐在座位上）
    const fwd = isTheatre ? { x: 0, z: -1 } : { x: -Math.cos(m.ang), z: -Math.sin(m.ang) }
    dummy.position.set(m.x + fwd.x * 0.12, m.y + 0.92 + (i % 3) * 0.02, m.z + fwd.z * 0.12)
    dummy.updateMatrix()
    crowdMesh.setMatrixAt(i, dummy.matrix)
    col.setHex(CROWD_COLORS[i % CROWD_COLORS.length])
    crowdMesh.setColorAt(i, col)
  })
  crowdMesh.instanceMatrix.needsUpdate = true
  if (crowdMesh.instanceColor) crowdMesh.instanceColor.needsUpdate = true
  scene.add(crowdMesh)
}

function buildSeats() {
  // 清理旧座位
  if (seatBaseMesh) { scene.remove(seatBaseMesh); seatBaseMesh = null }
  if (seatBackMesh) { scene.remove(seatBackMesh); seatBackMesh = null }
  if (seatBaseGeo) seatBaseGeo.dispose()
  if (seatBackGeo) seatBackGeo.dispose()
  if (seatMat) seatMat.dispose()
  codeToIndex.clear()
  seatMeta = []
  occupiedSet.clear()
  ;(props.occupied || []).forEach(c => occupiedSet.add(c))

  // 未提供座位分区（系统安排座位场次）→ 使用默认布局展示场馆
  const sections = (props.sections && props.sections.length) ? props.sections : defaultSections()
  const totalSec = sections.length || 1
  let total = 0
  sections.forEach(s => { total += (Number(s.rows) || 0) * (Number(s.cols) || 0) })
  if (total === 0) return

  const preset = getPreset(props.model)
  const seatCfg = preset.seat

  // 按票价分层：越贵越靠近舞台（内圈低位），越便宜越靠外圈（高位俯视看台）——贴合真实场馆
  const tierOfSec = new Map()
  ;[...sections]
    .map((s, i) => ({ i, price: Number(s.price) || 0 }))
    .sort((a, b) => b.price - a.price)
    .forEach((s, rank) => tierOfSec.set(s.i, rank))

  // 两段式座椅几何：底座 + 靠背（靠背置于底座后上方）
  seatBaseGeo = new THREE.BoxGeometry(0.58, 0.18, 0.56)
  seatBackGeo = new THREE.BoxGeometry(0.58, 0.46, 0.1)
  seatBackGeo.translate(0, 0.18 / 2 + 0.46 / 2, -0.56 / 2 + 0.1 / 2)
  seatMat = new THREE.MeshStandardMaterial({ roughness: 0.85, metalness: 0.05 })

  seatBaseMesh = new THREE.InstancedMesh(seatBaseGeo, seatMat, total)
  seatBackMesh = new THREE.InstancedMesh(seatBackGeo, seatMat.clone(), total)
  seatBaseMesh.castShadow = true; seatBaseMesh.receiveShadow = true
  seatBackMesh.castShadow = true

  const dummy = new THREE.Object3D()
  const col = new THREE.Color()
  venueOuterR = 0; venueTopY = 0
  let i = 0
  sections.forEach((sec, secIdx) => {
    const rows = Number(sec.rows) || 0
    const cols = Number(sec.cols) || 0
    for (let r = 1; r <= rows; r++) {
      for (let c = 1; c <= cols; c++) {
        const code = `${sec.id}-${r}-${c}`
        const p = seatPosition(secIdx, totalSec, r, rows, c, cols, seatCfg, tierOfSec.get(secIdx) || 0)
        const rotY = seatCfg.type === 'rect' ? Math.PI : (-p.ang - Math.PI / 2)
        dummy.position.set(p.x, p.y, p.z)
        dummy.rotation.set(0, rotY, 0)
        dummy.updateMatrix()
        seatBaseMesh.setMatrixAt(i, dummy.matrix)
        seatBackMesh.setMatrixAt(i, dummy.matrix)
        const isSold = occupiedSet.has(code)
        const tIdx = tierOfSec.get(secIdx) || 0
        col.setHex(isSold ? 0x595f6b : tierColor(tIdx))
        seatBaseMesh.setColorAt(i, col)
        seatBackMesh.setColorAt(i, col)
        codeToIndex.set(code, i)
        seatMeta.push({ code, ...p, rowFrac: rows > 1 ? (r - 1) / (rows - 1) : 0, tier: tIdx })
        venueOuterR = Math.max(venueOuterR, p.radius || Math.max(Math.abs(p.x), Math.abs(p.z)))
        venueTopY = Math.max(venueTopY, p.y)
        i++
      }
    }
  })
  seatBaseMesh.instanceMatrix.needsUpdate = true
  seatBackMesh.instanceMatrix.needsUpdate = true
  if (seatBaseMesh.instanceColor) seatBaseMesh.instanceColor.needsUpdate = true
  if (seatBackMesh.instanceColor) seatBackMesh.instanceColor.needsUpdate = true
  scene.add(seatBaseMesh)
  scene.add(seatBackMesh)

  buildCrowd()
  buildSectionLabels(sections, seatCfg, preset, totalSec, tierOfSec)
  refreshHighlight()

  // 若父组件已有选中座位 → 直接进入该座位第一人称视角
  if (props.selectedSeatCode && codeToIndex.has(props.selectedSeatCode)) {
    focusCode.value = props.selectedSeatCode
    focusLabel.value = seatLabelText(props.selectedSeatCode, sections)
    mode.value = 'seat'
    applySeatCamera(props.selectedSeatCode, true)
  }
}

function seatLabelText(code, sections) {
  const p = parseCode(code)
  if (!p) return code
  const sec = (sections || []).find(s => s.id === p.secId)
  return `${sec ? sec.name : p.secId}区 ${p.row}排${p.col}座`
}

function refreshHighlight() {
  if (!seatBaseMesh) return
  const col = new THREE.Color()
  seatMeta.forEach((m, i) => {
    col.setHex(occupiedSet.has(m.code) ? 0x595f6b : tierColor(m.tier))
    seatBaseMesh.setColorAt(i, col)
    seatBackMesh.setColorAt(i, col)
  })
  const hi = new Set()
  if (props.selectedSeatCode) hi.add(props.selectedSeatCode)
  if (focusCode.value) hi.add(focusCode.value)
  hi.forEach(code => {
    const idx = codeToIndex.get(code)
    if (idx != null) {
      seatBaseMesh.setColorAt(idx, COLOR.sel)
      seatBackMesh.setColorAt(idx, COLOR.sel)
    }
  })
  if (seatBaseMesh.instanceColor) { seatBaseMesh.instanceColor.needsUpdate = true; seatBackMesh.instanceColor.needsUpdate = true }
}

function buildScene() {
  const wrap = canvasWrap.value
  const w = wrap.clientWidth || 600
  const h = wrap.clientHeight || 420

  scene = new THREE.Scene()
  const preset = getPreset(props.model)
  applySky(preset.sky)

  camera = new THREE.PerspectiveCamera(55, w / h, 0.1, 1000)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(w, h)
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.outputColorSpace = THREE.SRGBColorSpace
  wrap.appendChild(renderer.domElement)

  // 灯光：半球光 + 环境光 + 主方向光（投影）+ 舞台暖色聚光
  scene.add(new THREE.HemisphereLight(0xbfd4ff, 0x202830, 0.65))
  scene.add(new THREE.AmbientLight(0xffffff, 0.25))
  const key = new THREE.DirectionalLight(0xffffff, 1.0)
  key.position.set(30, 60, 20)
  key.castShadow = true
  key.shadow.mapSize.set(1024, 1024)
  key.shadow.camera.near = 1
  key.shadow.camera.far = 320
  const d = 100
  key.shadow.camera.left = -d; key.shadow.camera.right = d
  key.shadow.camera.top = d; key.shadow.camera.bottom = -d
  key.shadow.bias = -0.0005
  scene.add(key)
  const spot = new THREE.SpotLight(0xfff0d0, 1.4, 160, Math.PI / 5, 0.4, 1)
  spot.position.set(0, 44, 0)
  spot.target.position.set(0, 0, 0)
  scene.add(spot); scene.add(spot.target)

  // 地面
  const ground = new THREE.Mesh(
    new THREE.CircleGeometry(140, 64),
    new THREE.MeshStandardMaterial({ color: 0x141a26, roughness: 1, metalness: 0 })
  )
  ground.rotation.x = -Math.PI / 2
  ground.position.y = 0
  ground.receiveShadow = true
  scene.add(ground)

  buildSeats()
  buildDecor(preset)

  // 全景初始相机距离按场馆实际外径自适应（分层后场馆更大）
  pano.radius = Math.max(getPreset(props.model).panoRadius, venueOuterR + 16)
  pano.theta = Math.PI / 2.6
  pano.phi = 0.95
  applyPanoCamera(true)
  bindEvents()
  animate()
}

// ---- 相机 ----
function applyPanoCamera(instant = false) {
  const r = pano.radius
  const pos = new THREE.Vector3(
    pano.target.x + r * Math.sin(pano.phi) * Math.cos(pano.theta),
    pano.target.y + r * Math.cos(pano.phi),
    pano.target.z + r * Math.sin(pano.phi) * Math.sin(pano.theta)
  )
  setFov(55)
  if (instant) {
    camera.position.copy(pos)
    camLook.copy(pano.target)
    camera.lookAt(camLook)
  } else {
    flyTo(pos, pano.target.clone())
  }
}

function applySeatCamera(code, instant = false) {
  const m = seatMeta.find(s => s.code === code)
  if (!m) return
  const preset = getPreset(props.model)
  const seatCfg = preset.seat
  // 人眼高度 ≈ 座位上方 1.45m
  const pos = new THREE.Vector3(m.x, m.y + 1.45, m.z)
  // 视野随排数/角度差异化：前排仰视（看吊屏/演员上半身），后排俯视台面；
  // 视线看向舞台靠自己一侧的表演区，左右座位的画面明显不同
  const frac = m.rowFrac != null ? m.rowFrac : 0
  let look
  if (seatCfg.type === 'rect') {
    // 剧院：台面在 -z 方向，视线向舞台中心汇聚
    const lookY = 5.4 - frac * 3.2
    look = new THREE.Vector3(m.x * 0.35, lookY, seatCfg.stageFrontZ - 2)
  } else {
    // 环形场馆：看向舞台内侧（半径 4.2 处的表演区）
    const lookY = (preset.stageLookAt[1] + 1.8) - frac * 2.8
    const lr = 4.2
    look = new THREE.Vector3(Math.cos(m.ang) * lr, lookY, Math.sin(m.ang) * lr)
  }
  setFov(68)
  if (instant) {
    camera.position.copy(pos)
    camLook.copy(look)
    camera.lookAt(camLook)
  } else {
    flyTo(pos, look)
  }
}

function setFov(f) {
  if (camera && camera.fov !== f) {
    camera.fov = f
    camera.updateProjectionMatrix()
  }
}

// 平滑飞行过渡（easeInOutCubic）
function flyTo(toPos, toLook, dur = 950) {
  camTween = {
    fromPos: camera.position.clone(),
    toPos: toPos.clone(),
    fromLook: camLook.clone(),
    toLook: toLook.clone(),
    t0: performance.now(),
    dur
  }
}
function stepTween(now) {
  if (!camTween) return false
  let k = Math.min(1, (now - camTween.t0) / camTween.dur)
  const e = k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2
  camera.position.lerpVectors(camTween.fromPos, camTween.toPos, e)
  camLook.lerpVectors(camTween.fromLook, camTween.toLook, e)
  camera.lookAt(camLook)
  if (k >= 1) camTween = null
  return true
}

// 大屏呼吸亮度（不再全屏变色，避免"纯绿色假屏"）
let screenT = 0
function animateScreens(dt) {
  screenT += dt
  const k = 0.62 + 0.22 * Math.sin(screenT * 1.5)
  screenMats.forEach(m => { if (m) m.emissiveIntensity = k })
}

function animate() {
  raf = requestAnimationFrame(animate)
  const now = performance.now()
  const dt = 0.016
  animateScreens(dt)
  if (camTween) {
    stepTween(now)
  } else if (mode.value === 'pano') {
    pano.theta += 0.0016
    const preset = getPreset(props.model)
    const r = pano.radius
    camera.position.set(
      pano.target.x + r * Math.sin(pano.phi) * Math.cos(pano.theta),
      pano.target.y + r * Math.cos(pano.phi),
      pano.target.z + r * Math.sin(pano.phi) * Math.sin(pano.theta)
    )
    camLook.copy(pano.target)
    camera.lookAt(camLook)
  }
  renderer.render(scene, camera)
}

// ---- 交互：拖拽旋转 / 滚轮缩放 / 点击选座 ----
let dragging = false
let lastX = 0, lastY = 0
function bindEvents() {
  const el = renderer.domElement
  el.style.cursor = 'grab'
  el.addEventListener('pointerdown', e => { dragging = true; lastX = e.clientX; lastY = e.clientY; el.style.cursor = 'grabbing' })
  window.addEventListener('pointerup', () => { dragging = false; el.style.cursor = 'grab' })
  window.addEventListener('pointermove', e => {
    if (!dragging || mode.value !== 'pano' || camTween) return
    const dx = e.clientX - lastX, dy = e.clientY - lastY
    lastX = e.clientX; lastY = e.clientY
    pano.theta -= dx * 0.005
    pano.phi = Math.max(0.25, Math.min(1.45, pano.phi - dy * 0.005))
  })
  el.addEventListener('wheel', e => {
    e.preventDefault()
    if (mode.value !== 'pano' || camTween) return
    pano.radius = Math.max(16, Math.min(120, pano.radius + e.deltaY * 0.03))
  }, { passive: false })
  el.addEventListener('click', onPick)
}

function onPick(e) {
  if (!seatBaseMesh) return
  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((e.clientX - rect.left) / rect.width) * 2 - 1,
    -((e.clientY - rect.top) / rect.height) * 2 + 1
  )
  const ray = new THREE.Raycaster()
  ray.setFromCamera(mouse, camera)
  const hit = ray.intersectObject(seatBaseMesh)
  if (hit.length) {
    const idx = hit[0].instanceId
    const m = seatMeta[idx]
    if (!m) return
    // 已售座位：提示不可选（贴合真实购票场景），不切换视角
    if (occupiedSet.has(m.code)) {
      ElMessage.warning('该座位已售出，换个座位预览视野吧')
      return
    }
    focusCode.value = m.code
    focusLabel.value = seatLabelText(m.code, props.sections && props.sections.length ? props.sections : defaultSections())
    focusSoldHint.value = false
    // 点击座位直接切换到该座位第一人称视角（可连续点击不同座位快速比较）
    mode.value = 'seat'
    applySeatCamera(m.code)
  }
}

function onResize() {
  if (!renderer || !canvasWrap.value) return
  const w = canvasWrap.value.clientWidth
  const h = canvasWrap.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

function disposeAll() {
  if (raf) cancelAnimationFrame(raf)
  if (ro) { ro.disconnect(); ro = null }
  sectionSprites.forEach(sp => {
    if (sp.material) {
      if (sp.material.map) sp.material.map.dispose()
      sp.material.dispose()
    }
  })
  sectionSprites = []
  screenMats.length = 0
  if (seatBaseMesh) scene && scene.remove(seatBaseMesh)
  if (seatBackMesh) scene && scene.remove(seatBackMesh)
  if (crowdMesh) scene && scene.remove(crowdMesh)
  if (seatBaseGeo) seatBaseGeo.dispose()
  if (seatBackGeo) seatBackGeo.dispose()
  if (seatMat) seatMat.dispose()
  if (crowdGeo) crowdGeo.dispose()
  if (crowdMat) crowdMat.dispose()
  seatBaseMesh = seatBackMesh = crowdMesh = null
  seatBaseGeo = seatBackGeo = crowdGeo = crowdMat = seatMat = null
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
    renderer = null
  }
  scene = null
}

let ro
onMounted(async () => {
  await nextTick()
  buildScene()
  ro = new ResizeObserver(onResize)
  ro.observe(canvasWrap.value)
})

onBeforeUnmount(() => {
  disposeAll()
})

watch(() => props.sections, () => { if (scene) buildSeats() }, { deep: false })
watch(() => props.occupied, () => { if (scene) buildSeats() }, { deep: false })
watch(() => props.model, () => {
  // 场馆模型切换：整体重建场景
  if (renderer) {
    disposeAll()
    nextTick(buildScene)
  }
})
// 父组件已选座位变化 → 自动飞入该座位第一人称视角
watch(() => props.selectedSeatCode, v => {
  if (!v) return
  focusCode.value = v
  focusLabel.value = seatLabelText(v, props.sections && props.sections.length ? props.sections : defaultSections())
  focusSoldHint.value = false
  mode.value = 'seat'
  if (camera) applySeatCamera(v)
})
watch(mode, m => {
  if (m === 'seat' && focusCode.value) applySeatCamera(focusCode.value)
  else if (m === 'pano') applyPanoCamera()
})
</script>

<style scoped>
.venue3d { position: relative; width: 100%; height: 100%; }
.venue3d-canvas { width: 100%; height: 420px; border-radius: 8px; overflow: hidden; background: #0a0f1c; }
.venue3d-canvas :deep(canvas) { display: block; }
.venue3d-bar { display: flex; align-items: center; gap: 12px; margin-top: 10px; flex-wrap: wrap; }
.venue3d-venue { font-size: 13px; font-weight: 600; color: #409eff; background: #ecf5ff; padding: 2px 10px; border-radius: 12px; }
.venue3d-ver { margin-left: 5px; font-style: normal; font-size: 11px; font-weight: 500; color: #7aa7d9; }
.venue3d-hint { font-size: 12px; color: #909399; }
.venue3d-legend { position: absolute; top: 10px; left: 10px; display: flex; gap: 12px; background: rgba(0,0,0,0.35); padding: 6px 10px; border-radius: 6px; }
.venue3d-legend span { font-size: 12px; color: #e6e6e6; display: inline-flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
.dot-avail { background: #4a90d9; }
.dot-sold { background: #595f6b; }
.dot-sel { background: #ffcc33; }
</style>
