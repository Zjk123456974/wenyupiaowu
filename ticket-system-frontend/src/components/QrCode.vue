<template>
  <div class="qr-code" :style="{ width: size + 'px', height: size + 'px' }">
    <img v-if="dataUrl" :src="dataUrl" :width="size" :height="size" alt="核销二维码" />
    <div v-else class="qr-loading">生成中…</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  // 待编码内容（后端签名的时效令牌）
  text: { type: String, default: '' },
  size: { type: Number, default: 120 }
})

const dataUrl = ref('')

const render = async () => {
  if (!props.text) {
    dataUrl.value = ''
    return
  }
  try {
    dataUrl.value = await QRCode.toDataURL(props.text, {
      width: props.size * 2,
      margin: 1,
      errorCorrectionLevel: 'M'
    })
  } catch (e) {
    dataUrl.value = ''
  }
}

onMounted(render)
watch(() => props.text, render)
</script>

<style scoped>
.qr-code {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 4px;
  box-sizing: border-box;
}
.qr-code img {
  display: block;
}
.qr-loading {
  font-size: 12px;
  color: #909399;
}
</style>
