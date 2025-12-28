<template>
  <div class="wrap">
    <div class="card">
      <h2>注册</h2>
      <input v-model="username" placeholder="用户名（8-20个字符）" />
      <input v-model="email" placeholder="邮箱" type="email" />
      <div class="password-wrapper">
        <input 
          v-model="password" 
          :type="showPassword ? 'text' : 'password'" 
          placeholder="密码" 
        />
        <span class="password-toggle" @click="togglePassword">
          {{ showPassword ? '👁️' : '👁️‍🗨️' }}
        </span>
      </div>
      <div class="password-requirements">
        密码要求：至少8位，包含字母、数字和特殊字符(@$%!%*#?&_)
      </div>
      <div class="password-wrapper">
        <input 
          v-model="confirmPassword" 
          :type="showConfirmPassword ? 'text' : 'password'" 
          placeholder="确认密码" 
        />
        <span class="password-toggle" @click="toggleConfirmPassword">
          {{ showConfirmPassword ? '👁️' : '👁️‍🗨️' }}
        </span>
      </div>
      <button @click="doRegister" :disabled="loading">{{ loading ? '处理中...' : '注册' }}</button>
      <p class="tip" v-if="error">{{ error }}</p>
      <p class="link">
        <a @click="goToLogin">已有账号？去登录</a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { register } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

function togglePassword() {
  showPassword.value = !showPassword.value
}

function toggleConfirmPassword() {
  showConfirmPassword.value = !showConfirmPassword.value
}

function validateForm() {
  if (!username.value.trim()) {
    error.value = '请输入用户名'
    return false
  }
  // 验证用户名长度
  if (username.value.trim().length < 8 || username.value.trim().length > 20) {
    error.value = '用户名长度必须在8-20个字符之间'
    return false
  }
  if (!email.value.trim()) {
    error.value = '请输入邮箱'
    return false
  }
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.value)) {
    error.value = '请输入有效的邮箱地址'
    return false
  }
  if (!password.value) {
    error.value = '请输入密码'
    return false
  }
  if (!confirmPassword.value) {
    error.value = '请输入确认密码'
    return false
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return false
  }
  if (password.value.length < 8) {
    error.value = '密码长度至少8位'
    return false
  }
  // 验证密码复杂度
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$%!%*#?&_])[A-Za-z\d@$%!%*#?&_]{8,}$/
  if (!passwordRegex.test(password.value)) {
    error.value = '密码必须包含字母、数字和特殊字符(@$%!%*#?&_)'
    return false
  }
  return true
}

async function doRegister() {
  error.value = ''
  if (!validateForm()) {
    return
  }
  
  loading.value = true
  try {
    const res = await register({ 
      username: username.value.trim(), 
      password: password.value, 
      confirmPassword: confirmPassword.value,
      email: email.value 
    })
    
    // 检查业务状态码：后端在 HTTP 200 响应中返回业务状态码
    if (res.data.code === 200) {
      // 注册成功后跳转到登录页面
      router.push('/login?registered=true')
    } else {
      // 业务状态码不是 200，说明注册失败
      // 显示后端返回的具体错误信息
      error.value = res.data.message || '注册失败'
    }
  } catch (e: any) {
    // 处理网络错误或其他异常
    let errorMessage = '注册失败'
    if (e.response?.data?.message) {
      errorMessage = e.response.data.message
    } else if (e.response?.data?.error) {
      errorMessage = e.response.data.error
    } else if (e.message) {
      errorMessage = e.message
    }
    error.value = errorMessage
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.wrap { 
  display: flex; 
  height: 100vh; 
  align-items: center; 
  justify-content: center; 
  background: #111; 
}

.card { 
  width: 320px; 
  background: #1a1a1a; 
  padding: 24px; 
  border-radius: 8px; 
  color: #ddd; 
  display: flex; 
  flex-direction: column; 
  gap: 12px; 
}

input { 
  padding: 8px; 
  background: #111; 
  border: 1px solid #333; 
  color: #eee; 
  border-radius: 4px; 
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  flex: 1;
  padding-right: 32px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  cursor: pointer;
  user-select: none;
  font-size: 16px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.password-toggle:hover {
  opacity: 1;
}

.password-requirements {
  font-size: 12px;
  color: #aaa;
  margin-top: -8px;
  margin-bottom: 8px;
}

button { 
  padding: 10px; 
  background: #3b82f6; 
  color: white; 
  border: none; 
  border-radius: 4px; 
  cursor: pointer; 
}

button:disabled {
  background: #666;
  cursor: not-allowed;
}

.tip { 
  color: #ff8; 
  font-size: 14px;
}

.link {
  text-align: center;
  margin-top: 8px;
}

.link a {
  color: #3b82f6;
  cursor: pointer;
  text-decoration: none;
}

.link a:hover {
  text-decoration: underline;
}
</style>
