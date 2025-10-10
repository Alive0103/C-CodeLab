<template>
  <div class="wrap">
    <div class="card">
      <h2>登录</h2>
      <input v-model="username" placeholder="用户名" />
      <input v-model="password" placeholder="密码" type="password" />
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

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const registered = ref(false)

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
    const user = res.data.data
    if (user) {
      // 后端使用session管理，存储用户信息到store
      auth.setUser(user)
      router.push('/editor')
    } else {
      error.value = '登录失败'
    }
  } catch (e: any) {
    console.error('Login error:', e)
    error.value = e.response?.data?.message || '登录失败'
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


