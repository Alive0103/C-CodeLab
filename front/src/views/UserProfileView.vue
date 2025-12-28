<template>
  <div class="user-profile">
    <div class="profile-container">
      <h1>个人信息管理</h1>
      
      <!-- 基本信息 -->
      <div class="section">
        <h2>基本信息</h2>
        <div class="form-group">
          <label>用户名</label>
          <input v-model="profile.username" placeholder="用户名（8-20个字符）" />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="profile.email" type="email" disabled class="disabled-input" />
          <div class="field-hint">邮箱不可修改，作为账号的唯一标识</div>
        </div>
        <div class="form-group">
          <label>注册时间</label>
          <input :value="formatDate(profile.createdAt)" disabled class="disabled-input" />
        </div>
        <button @click="updateProfile" :disabled="profileLoading" class="btn-primary">
          {{ profileLoading ? '更新中...' : '更新信息' }}
        </button>
        <!-- 基本信息提示 -->
        <div v-if="profileMessage" :class="['section-message', profileMessageType]">
          {{ profileMessage }}
        </div>
      </div>

      <!-- 修改密码 -->
      <div class="section">
        <h2>修改密码</h2>
        <div class="form-group">
          <label>原密码</label>
          <div class="password-wrapper">
            <input 
              v-model="passwordForm.oldPassword" 
              :type="showOldPassword ? 'text' : 'password'" 
            />
            <span class="password-toggle" @click="toggleOldPassword">
              {{ showOldPassword ? '👁️' : '👁️‍🗨️' }}
            </span>
          </div>
        </div>
        <div class="form-group">
          <label>新密码</label>
          <div class="password-wrapper">
            <input 
              v-model="passwordForm.newPassword" 
              :type="showNewPassword ? 'text' : 'password'" 
            />
            <span class="password-toggle" @click="toggleNewPassword">
              {{ showNewPassword ? '👁️' : '👁️‍🗨️' }}
            </span>
          </div>
          <div class="password-requirements">
            密码要求：至少8位，包含字母、数字和特殊字符(@$%!%*#?&_)
          </div>
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <div class="password-wrapper">
            <input 
              v-model="passwordForm.confirmPassword" 
              :type="showConfirmPassword ? 'text' : 'password'" 
            />
            <span class="password-toggle" @click="toggleConfirmPassword">
              {{ showConfirmPassword ? '👁️' : '👁️‍🗨️' }}
            </span>
          </div>
        </div>
        <button @click="changePassword" :disabled="passwordLoading" class="btn-primary">
          {{ passwordLoading ? '修改中...' : '修改密码' }}
        </button>
        <!-- 密码修改提示 -->
        <div v-if="passwordMessage" :class="['section-message', passwordMessageType]">
          {{ passwordMessage }}
        </div>
      </div>

      <!-- 操作记录 -->
      <div class="section">
        <h2>快速操作</h2>
        <div class="quick-actions">
          <button @click="goToCodeHistory" class="btn-secondary">查看代码历史</button>
          <button @click="goToEditor" class="btn-secondary">返回编辑器</button>
          <button @click="handleLogout" class="btn-danger">登出</button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getUserProfile, updateProfile as updateProfileApi, changePassword as changePasswordApi } from '../api/user'

const router = useRouter()
const auth = useAuthStore()

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
const profileMessage = ref('')
const profileMessageType = ref<'success' | 'error'>('success')
const passwordMessage = ref('')
const passwordMessageType = ref<'success' | 'error'>('success')
const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

function toggleOldPassword() {
  showOldPassword.value = !showOldPassword.value
}

function toggleNewPassword() {
  showNewPassword.value = !showNewPassword.value
}

function toggleConfirmPassword() {
  showConfirmPassword.value = !showConfirmPassword.value
}

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  try {
    const res = await getUserProfile()
    profile.value = res.data.data
  } catch (error) {
    console.error('加载用户信息失败:', error)
    profileMessage.value = '加载用户信息失败'
    profileMessageType.value = 'error'
    setTimeout(() => {
      profileMessage.value = ''
    }, 3000)
  }
}

async function updateProfile() {
  // 验证用户名
  if (!profile.value.username.trim()) {
    profileMessage.value = '用户名不能为空'
    profileMessageType.value = 'error'
    setTimeout(() => {
      profileMessage.value = ''
    }, 3000)
    return
  }
  
  // 验证用户名长度
  if (profile.value.username.trim().length < 8 || profile.value.username.trim().length > 20) {
    profileMessage.value = '用户名长度必须在8-20个字符之间'
    profileMessageType.value = 'error'
    setTimeout(() => {
      profileMessage.value = ''
    }, 3000)
    return
  }

  profileLoading.value = true
  profileMessage.value = ''
  try {
    const res = await updateProfileApi({ username: profile.value.username.trim() })
    
    console.log('更新信息响应:', res)
    console.log('响应结构:', {
      hasRes: !!res,
      hasData: !!res?.data,
      resDataType: typeof res?.data,
      resDataKeys: res?.data ? Object.keys(res.data) : [],
      fullRes: res
    })
    
    // 安全检查：确保响应存在
    if (!res) {
      console.error('更新信息响应为空:', res)
      profileMessage.value = '更新失败：未收到服务器响应'
      profileMessageType.value = 'error'
      setTimeout(() => {
        profileMessage.value = ''
      }, 3000)
      return
    }
    
    if (!res.data) {
      console.error('更新信息响应数据为空:', res)
      profileMessage.value = '更新失败：服务器响应格式错误'
      profileMessageType.value = 'error'
      setTimeout(() => {
        profileMessage.value = ''
      }, 3000)
      return
    }
    
    // 检查业务状态码
    if (res.data.code === 200) {
      profileMessage.value = '信息更新成功，请重新登录'
      profileMessageType.value = 'success'
      // 清除token，跳转到登录页面
      localStorage.removeItem('token')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      profileMessage.value = res.data.message || '更新失败'
      profileMessageType.value = 'error'
      setTimeout(() => {
        profileMessage.value = ''
      }, 3000)
    }
  } catch (error: any) {
    console.error('更新信息异常:', error)
    let errorMessage = '更新失败'
    if (error?.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error?.response?.data?.error) {
      errorMessage = typeof error.response.data.error === 'string' 
        ? error.response.data.error 
        : error.response.data.error.message || '更新失败'
    } else if (error?.response?.data?.code) {
      errorMessage = error.response.data.message || '更新失败'
    } else if (error?.message) {
      const techErrorPatterns = [
        /Cannot read properties/i,
        /undefined/i,
        /null/i,
        /TypeError/i,
        /ReferenceError/i
      ]
      const isTechError = techErrorPatterns.some(pattern => pattern.test(error.message))
      if (isTechError) {
        errorMessage = '更新失败：请检查网络连接或稍后重试'
      } else {
        errorMessage = error.message
      }
    }
    profileMessage.value = errorMessage
    profileMessageType.value = 'error'
    setTimeout(() => {
      profileMessage.value = ''
    }, 3000)
  } finally {
    profileLoading.value = false
  }
}

async function changePassword() {
  if (!passwordForm.value.oldPassword) {
    passwordMessage.value = '请输入原密码'
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
    return
  }
  if (!passwordForm.value.newPassword) {
    passwordMessage.value = '请输入新密码'
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordMessage.value = '两次输入的新密码不一致'
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
    return
  }
  if (passwordForm.value.newPassword.length < 8) {
    passwordMessage.value = '新密码长度至少8位'
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
    return
  }

  // 验证密码复杂度
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$%!%*#?&_])[A-Za-z\d@$%!%*#?&_]{8,}$/
  if (!passwordRegex.test(passwordForm.value.newPassword)) {
    passwordMessage.value = '密码必须包含字母、数字和特殊字符(@$%!%*#?&_)'
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
    return
  }

  passwordLoading.value = true
  passwordMessage.value = ''
  try {
    const res = await changePasswordApi({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    
    // 安全检查：确保响应存在
    if (!res) {
      console.error('密码修改响应为空:', res)
      passwordMessage.value = '密码修改失败：未收到服务器响应'
      passwordMessageType.value = 'error'
      setTimeout(() => {
        passwordMessage.value = ''
      }, 3000)
      return
    }
    
    if (!res.data) {
      console.error('密码修改响应数据为空:', res)
      passwordMessage.value = '密码修改失败：服务器响应格式错误'
      passwordMessageType.value = 'error'
      setTimeout(() => {
        passwordMessage.value = ''
      }, 3000)
      return
    }
    
    // 检查业务状态码：后端在 HTTP 200 响应中返回业务状态码
    if (res.data.code === 200) {
      passwordMessage.value = '密码修改成功，即将跳转到登录页面'
      passwordMessageType.value = 'success'
      // 清空表单
      passwordForm.value = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      // 清除token，跳转到登录页面
      localStorage.removeItem('token')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      // 业务状态码不是 200，说明密码修改失败
      // 显示后端返回的具体错误信息
      passwordMessage.value = res.data.message || '密码修改失败'
      passwordMessageType.value = 'error'
      setTimeout(() => {
        passwordMessage.value = ''
      }, 3000)
    }
  } catch (error: any) {
    console.error('密码修改异常:', error)
    // 处理网络错误或其他异常
    let errorMessage = '密码修改失败'
    
    // 优先从后端响应中获取错误信息
    if (error?.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error?.response?.data?.error) {
      errorMessage = typeof error.response.data.error === 'string' 
        ? error.response.data.error 
        : error.response.data.error.message || '密码修改失败'
    } else if (error?.response?.data?.code) {
      // 如果有业务状态码，说明是业务错误
      errorMessage = error.response.data.message || '密码修改失败'
    } else if (error?.message) {
      // 如果是技术错误信息（包含 "Cannot read" 等），转换为友好提示
      const techErrorPatterns = [
        /Cannot read properties/i,
        /undefined/i,
        /null/i,
        /TypeError/i,
        /ReferenceError/i
      ]
      const isTechError = techErrorPatterns.some(pattern => pattern.test(error.message))
      if (isTechError) {
        errorMessage = '密码修改失败：请检查网络连接或稍后重试'
      } else {
        errorMessage = error.message
      }
    }
    
    passwordMessage.value = errorMessage
    passwordMessageType.value = 'error'
    setTimeout(() => {
      passwordMessage.value = ''
    }, 3000)
  } finally {
    passwordLoading.value = false
  }
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleString('zh-CN')
}

function goToCodeHistory() {
  router.push('/code-history')
}

function goToEditor() {
  router.push('/editor')
}

async function handleLogout() {
  if (confirm('确定要登出吗？')) {
    await auth.logout()
    router.push('/login')
  }
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

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  flex: 1;
  padding-right: 36px;
}

.password-toggle {
  position: absolute;
  right: 10px;
  cursor: pointer;
  user-select: none;
  font-size: 18px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.password-toggle:hover {
  opacity: 1;
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

.field-hint {
  font-size: 12px;
  color: #888;
  margin-top: 5px;
  font-style: italic;
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

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover {
  background: #b91c1c;
}

.quick-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.section-message {
  padding: 12px;
  border-radius: 4px;
  margin-top: 15px;
  font-weight: 500;
  font-size: 14px;
}

.section-message.success {
  background: #0f3d0f;
  color: #90ee90;
  border: 1px solid #22c55e;
}

.section-message.error {
  background: #3d0f0f;
  color: #ff6b6b;
  border: 1px solid #dc2626;
}
</style>
