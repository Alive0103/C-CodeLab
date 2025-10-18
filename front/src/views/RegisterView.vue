<template>
  <div class="wrap">
    <div class="card">
      <h2>注册</h2>
      <input v-model="username" placeholder="用户名" />
      <input v-model="email" placeholder="邮箱" type="email" />
      <input v-model="password" placeholder="密码" type="password" />
      <div class="password-requirements">
        密码要求：至少8位，包含字母、数字和特殊字符(@$%!%*#?&)
      </div>
      <input v-model="confirmPassword" placeholder="确认密码" type="password" />
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

function validateForm() {
  if (!username.value.trim()) {
    error.value = '请输入用户名'
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
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return false
  }
  if (password.value.length < 8) {
    error.value = '密码长度至少8位'
    return false
  }
  // 验证密码复杂度
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$%!%*#?&])[A-Za-z\d@$%!%*#?&]{8,}$/
  if (!passwordRegex.test(password.value)) {
    error.value = '密码必须包含字母、数字和特殊字符(@$%!%*#?&)'
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
    await register({ 
      username: username.value, 
      password: password.value, 
      email: email.value 
    })
    // 注册成功后跳转到登录页面
    router.push('/login?registered=true')
  } catch (e: any) {
    error.value = e.response?.data?.message || '注册失败'
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
