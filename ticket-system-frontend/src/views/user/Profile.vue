<template>
  <el-card shadow="never" class="profile-card">
    <template #header>
      <span class="card-title">个人信息</span>
    </template>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="max-width: 500px"
    >
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="头像" prop="avatar">
        <div class="avatar-uploader">
          <el-upload
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <span v-if="form.avatar" class="avatar-tip">点击更换头像</span>
          <span v-else class="avatar-tip">点击上传头像</span>
        </div>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="个人简介" prop="bio">
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="4"
          placeholder="请输入个人简介"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserInfo, updateUserInfo } from '@/api/user'
import { uploadImage } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  nickname: '',
  avatar: '',
  phone: '',
  bio: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ]
}

const fetchUserInfo = async () => {
  try {
    const res = await getUserInfo()
    const data = res.data
    form.nickname = data.nickname || ''
    form.avatar = data.avatar || ''
    form.phone = data.phone || ''
    form.bio = data.bio || ''
  } catch {
    // 静默处理
  }
}

const handleAvatarUpload = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res = await uploadImage(formData)
    if (res.data) {
      form.avatar = res.data
      ElMessage.success('头像上传成功')
    }
  } catch {
    ElMessage.error('头像上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const isImg = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImg) {
    ElMessage.error('只能上传图片格式文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await updateUserInfo({
      nickname: form.nickname,
      avatar: form.avatar,
      phone: form.phone,
      bio: form.bio
    })
    userStore.updateProfile({
      nickname: form.nickname,
      avatar: form.avatar
    })
    ElMessage.success('保存成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-card {
  min-height: 400px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-uploader :deep(.el-upload) {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #e4e7ed;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #ff4d6d;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #c0c4cc;
}

.avatar-tip {
  font-size: 13px;
  color: #909399;
}
</style>
