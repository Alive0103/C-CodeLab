<template>
  <div class="user-profile">
    <div class="profile-container">
      <h1>个人信息管理</h1>
      
      <!-- 基本信息 -->
      <div class="section">
        <h2>基本信息</h2>
        <div class="form-group">
          <label>用户名</label>
          <input v-model="profile.username" disabled class="disabled-input" />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="profile.email" type="email" />
        </div>
        <div class="form-group">
          <label>注册时间</label>
          <input :value="formatDate(profile.createdAt)" disabled class="disabled-input" />
        </div>
        <button @click="updateProfile" :disabled="profileLoading" class="btn-primary">
          {{ profileLoading ? '更新中...' : '更新信息' }}
        </button>
      </div>

      <!-- 修改密码 -->
      <div class="section">
        <h2>修改密码</h2>
        <div class="form-group">
          <label>原密码</label>
          <input v-model="passwordForm.oldPassword" type="password" />
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="passwordForm.newPassword" type="password" />
          <div class="password-requirements">
            密码要求：至少8位，包含字母、数字和特殊字符(@$%!%*#?&)
          </div>
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input v-model="passwordForm.confirmPassword" type="password" />
        </div>
        <button @click="changePassword" :disabled="passwordLoading" class="btn-primary">
          {{ passwordLoading ? '修改中...' : '修改密码' }}
        </button>
      </div>

      <!-- 操作记录 -->
      <div class="section">
        <h2>快速操作</h2>
        <div class="quick-actions">
          <button @click="goToCodeHistory" class="btn-secondary">查看代码历史</button>
          <button @click="goToEditor" class="btn-secondary">返回编辑器</button>
        </div>
      </div>

      <!-- 消息提示 -->
      <div v-if="message" :class="['message', messageType]">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserProfile, updateProfile, changePassword } from '../api/user'

const router = useRouter()

const profile = ref({
  id: 0,
  username: '',
  email: '',
  createdAt: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileLoading = ref(false)
const passwordLoading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  try {
    const res = await getUserProfile()
    profile.value = res.data.data
  } catch (error) {
    console.error('加载用户信息失败:', error)
    showMessage('加载用户信息失败', 'error')
  }
}

async function updateProfile() {
  if (!profile.value.email.trim()) {
    showMessage('邮箱不能为空', 'error')
    return
  }

  profileLoading.value = true
  try {
    await updateProfile({ email: profile.value.email })
    showMessage('信息更新成功', 'success')
  } catch (error: any) {
    showMessage(error.response?.data?.message || '更新失败', 'error')
  } finally {
    profileLoading.value = false
  }
}

async function changePassword() {
  if (!passwordForm.value.oldPassword) {
    showMessage('请输入原密码', 'error')
    return
  }
  if (!passwordForm.value.newPassword) {
    showMessage('请输入新密码', 'error')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showMessage('两次输入的新密码不一致', 'error')
    return
  }
  if (passwordForm.value.newPassword.length < 8) {
    showMessage('新密码长度至少8位', 'error')
    return
  }

  // 验证密码复杂度
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$%!%*#?&])[A-Za-z\d@$%!%*#?&]{8,}$/
  if (!passwordRegex.test(passwordForm.value.newPassword)) {
    showMessage('密码必须包含字母、数字和特殊字符(@$%!%*#?&)', 'error')
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    showMessage('密码修改成功', 'success')
    // 清空表单
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error: any) {
    showMessage(error.response?.data?.message || '密码修改失败', 'error')
  } finally {
    passwordLoading.value = false
  }
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleString('zh-CN')
}

function showMessage(text: string, type: 'success' | 'error') {
  message.value = text
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

function goToCodeHistory() {
  router.push('/code-history')
}

function goToEditor() {
  router.push('/editor')
}
</script>

<style scoped>
.user-profile {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #1a1a1a;
  min-height: 100vh;
  color: #ddd;
}

.profile-container h1 {
  color: #fff;
  margin-bottom: 30px;
  text-align: center;
}

.section {
  background: #222;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 30px;
}

.section h2 {
  color: #fff;
  margin-bottom: 20px;
  border-bottom: 2px solid #3b82f6;
  padding-bottom: 10px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #ccc;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  background: #111;
  border: 1px solid #333;
  color: #eee;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
}

.disabled-input {
  background: #333 !important;
  color: #888 !important;
  cursor: not-allowed;
}

.password-requirements {
  font-size: 12px;
  color: #aaa;
  margin-top: 5px;
}

.btn-primary, .btn-secondary {
  padding: 12px 24px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  background: #666;
  cursor: not-allowed;
}

.btn-secondary {
  background: #444;
  color: #ddd;
  margin-right: 10px;
}

.btn-secondary:hover {
  background: #555;
}

.quick-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.message {
  padding: 15px;
  border-radius: 4px;
  margin-top: 20px;
  font-weight: 500;
}

.message.success {
  background: #0f3d0f;
  color: #90ee90;
  border: 1px solid #22c55e;
}

.message.error {
  background: #3d0f0f;
  color: #ff6b6b;
  border: 1px solid #dc2626;
}
</style>
