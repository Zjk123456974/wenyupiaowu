<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- 品牌展示区 -->
      <div class="brand-panel">
        <div class="brand-inner">
          <div class="brand-badge" @click="$router.push('/')">
            <img src="/logo.jpg" class="badge-logo-img" alt="logo" />
            <span>票务在线</span>
          </div>
          <h1 class="brand-title">发现精彩<br />抢到热爱</h1>
          <p class="brand-slogan">演唱会 · 话剧 · 展览 · 体育赛事<br />一站式文娱活动预约抢票平台</p>

          <ul class="brand-features">
            <li><span class="feature-icon"><el-icon><Stopwatch /></el-icon></span>热门演出 准时开抢 · 快人一步</li>
            <li><span class="feature-icon"><el-icon><Iphone /></el-icon></span>电子票夹 随取随用 · 安全核销</li>
            <li><span class="feature-icon"><el-icon><Bell /></el-icon></span>开售提醒 降价通知 · 不错过</li>
          </ul>

          <div class="brand-deco deco-1"></div>
          <div class="brand-deco deco-2"></div>
          <div class="brand-deco deco-3"></div>
        </div>
      </div>

      <!-- 表单区 -->
      <div class="form-panel">
        <div class="form-inner">
          <div class="form-head">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-sub">登录后可预约、抢票并管理您的电子票</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名 / 手机号"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>

          <div class="form-extra">
            <el-link type="primary" :underline="false" @click="$router.push('/register')">还没有账号？立即注册</el-link>
            <span class="back-home" @click="$router.push('/')">← 返回首页</span>
          </div>

          <div class="role-tips">
            <span class="tip-item"><span class="dot" style="background:#409eff"></span>普通用户购票观演</span>
            <span class="tip-item"><span class="dot" style="background:#67c23a"></span>运营方发布演出</span>
            <span class="tip-item"><span class="dot" style="background:#ff8a3d"></span>管理员平台治理</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      username: form.username,
      password: form.password
    })
    userStore.setLoginInfo(res.data)
    ElMessage.success('登录成功')
    const role = Number(res.data.role)
    if (role === 2) {
      router.push('/admin/dashboard')
    } else if (role === 1) {
      router.push('/operator/dashboard')
    } else {
      router.push('/')
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2fb;
  padding: 24px;
}

.auth-shell {
  width: 960px;
  max-width: 100%;
  min-height: 600px;
  display: flex;
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 24px 70px rgba(27, 42, 74, 0.18);
}

/* ===== 左侧品牌区 ===== */
.brand-panel {
  width: 44%;
  position: relative;
  background: linear-gradient(150deg, #22305c 0%, #2b4a8f 55%, #2b6cb8 100%);
  color: #fff;
  overflow: hidden;
}

.brand-inner {
  position: relative;
  z-index: 2;
  height: 100%;
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
}

.brand-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 1px;
  margin-bottom: 56px;
}

.badge-logo-img {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 6px 16px rgba(255, 77, 109, 0.4);
}

.brand-title {
  font-size: 38px;
  line-height: 1.35;
  letter-spacing: 3px;
  margin-bottom: 18px;
  font-weight: 800;
}

.brand-slogan {
  font-size: 15px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 40px;
}

.brand-features {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}

.feature-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

/* 装饰光斑 */
.brand-deco {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.deco-1 {
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(255, 122, 69, 0.35), transparent 65%);
  top: -60px;
  right: -70px;
}

.deco-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(64, 214, 255, 0.28), transparent 65%);
  bottom: 10%;
  left: -60px;
}

.deco-3 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(255, 212, 59, 0.22), transparent 65%);
  bottom: -30px;
  right: 12%;
}

/* ===== 右侧表单区 ===== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-inner {
  width: 340px;
}

.form-head {
  margin-bottom: 34px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #1b2a4a;
  margin-bottom: 8px;
}

.form-sub {
  font-size: 13px;
  color: #909399;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  letter-spacing: 6px;
  border-radius: 24px;
  margin-top: 6px;
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
  box-shadow: 0 8px 20px rgba(255, 77, 109, 0.28);
}

.submit-btn:hover {
  opacity: 0.92;
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
}

.back-home {
  font-size: 13px;
  color: #909399;
  cursor: pointer;
}

.back-home:hover {
  color: #409eff;
}

.role-tips {
  margin-top: 44px;
  padding-top: 20px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.tip-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #909399;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.form-inner :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 3px 14px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.form-inner :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ff4d6d inset;
}
</style>
