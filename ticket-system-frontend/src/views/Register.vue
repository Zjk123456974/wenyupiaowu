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
          <h1 class="brand-title">注册账号<br />开启观演之旅</h1>
          <p class="brand-slogan">注册即可预约心仪演出、参与限时抢票，<br />还能享受开售提醒等专属服务</p>

          <ul class="brand-features">
            <li><span class="feature-icon"><el-icon><MagicStick /></el-icon></span>千人千面 智能推荐热门演出</li>
            <li><span class="feature-icon"><el-icon><Clock /></el-icon></span>开售倒计时 预约提醒不错过</li>
            <li><span class="feature-icon"><el-icon><CircleCheck /></el-icon></span>电子票实名核验 安全有保障</li>
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
            <h2 class="form-title">创建账号</h2>
            <p class="form-sub">填写以下信息完成注册（用户名 3~20 个字符）</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            @submit.prevent="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
                maxlength="11"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请设置密码（6~20位）"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次确认密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form>

          <div class="form-extra">
            <span class="agree-text">注册即代表同意《用户协议》与《隐私政策》</span>
          </div>

          <div class="footer-line">
            已有账号？
            <el-link type="primary" :underline="false" @click="$router.push('/login')">直接登录</el-link>
            <span class="back-home" @click="$router.push('/')">← 返回首页</span>
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
import { User, Lock, Phone } from '@element-plus/icons-vue'
import { register } from '@/api/user'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名仅支持字母、数字、下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*]{6,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
  min-height: 640px;
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
  background: linear-gradient(150deg, #7a2b4a 0%, #b8375f 50%, #ff4d6d 115%);
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
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
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
  color: rgba(255, 255, 255, 0.85);
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
  color: rgba(255, 255, 255, 0.94);
}

.feature-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
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
  background: radial-gradient(circle, rgba(255, 212, 59, 0.3), transparent 65%);
  top: -60px;
  right: -70px;
}

.deco-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.18), transparent 65%);
  bottom: 10%;
  left: -60px;
}

.deco-3 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(64, 214, 255, 0.25), transparent 65%);
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
  margin-bottom: 26px;
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

.agree-text {
  font-size: 12px;
  color: #c0c4cc;
}

.form-extra {
  margin-top: 14px;
  text-align: center;
}

.footer-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed #ebeef5;
  font-size: 14px;
  color: #909399;
}

.back-home {
  margin-left: auto;
  font-size: 13px;
  color: #c0c4cc;
  cursor: pointer;
}

.back-home:hover {
  color: #409eff;
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
