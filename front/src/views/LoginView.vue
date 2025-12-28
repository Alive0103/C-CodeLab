<template>
  <div class="wrap">
    <div class="card">
      <h2>登录</h2>
      <input v-model="username" placeholder="用户名" />
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
      <button @click="doLogin" :disabled="loading">{{ loading ? '处理中...' : '登录' }}</button>
      <p class="tip" v-if="error">{{ error }}</p>
      <p class="success" v-if="registered">注册成功！请登录</p>
      <p class="link">
        <a @click="goToRegister">没有账号？去注册</a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { login } from '../api/auth'
import { getUserProfile } from '../api/user'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const registered = ref(false)
const showPassword = ref(false)

function togglePassword() {
  showPassword.value = !showPassword.value
}

onMounted(() => {
  // 检查是否从注册页面跳转过来
  if (route.query.registered === 'true') {
    registered.value = true
  }
})

async function doLogin() {
  error.value = ''
  loading.value = true
  try {
    const res = await login({ username: username.value, password: password.value })
    
    // 检查业务状态码：后端在 HTTP 200 响应中返回业务状态码
    // 成功时：code: 200, data: "token字符串"
    // 失败时：code: 401, message: "用户名或密码错误", data: null
    if (res.data.code === 200) {
      const token = res.data.data
      if (token) {
        // 存储JWT token到localStorage（确保包含Bearer前缀）
        localStorage.setItem('token', token)
        // 获取用户信息并存储到store
        const userRes = await getUserProfile()
        auth.setUser(userRes.data.data)
        router.push('/editor')
      } else {
        error.value = '登录失败：未获取到token'
      }
    } else {
      // 业务状态码不是 200，说明登录失败
      // 显示后端返回的具体错误信息
      error.value = res.data.message || '登录失败'
    }
  } catch (e: any) {
    // 处理网络错误或其他异常
    let errorMessage = '登录失败'
    
    // 从 axios 错误响应中提取 message
    if (e?.response?.data?.message) {
      errorMessage = e.response.data.message
    } else if (e?.response?.data?.error) {
      errorMessage = typeof e.response.data.error === 'string' 
        ? e.response.data.error 
        : e.response.data.error.message || '登录失败'
    } else if (e?.message) {
      errorMessage = e.message
    }
    
    error.value = errorMessage
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.wrap { display:flex; height:100vh; align-items:center; justify-content:center; background:#111; }
.card { width:320px; background:#1a1a1a; padding:24px; border-radius:8px; color:#ddd; display:flex; flex-direction:column; gap:12px; }
input { padding:8px; background:#111; border:1px solid #333; color:#eee; border-radius:4px; }
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
button { padding:10px; background:#3b82f6; color:white; border:none; border-radius:4px; cursor:pointer; }
.tip { color:#ff8; }
.success { color:#4ade80; font-size:14px; }
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


