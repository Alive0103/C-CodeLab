<template>
  <div class="wrap">
    <div class="card">
      <h2>登录</h2>
      <input v-model="username" placeholder="用户名" />
      <input v-model="password" placeholder="密码" type="password" />
      <button @click="doLogin" :disabled="loading">{{ loading ? '处理中...' : '登录' }}</button>
      <p class="tip" v-if="error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { login } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function doLogin() {
  error.value = ''
  loading.value = true
  try {
    const res = await login({ username: username.value, password: password.value })
    const token = res.data.data?.accessToken
    if (token) {
      auth.setToken(token)
      router.push('/editor')
    } else {
      error.value = '登录失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap { display:flex; height:100vh; align-items:center; justify-content:center; background:#111; }
.card { width:320px; background:#1a1a1a; padding:24px; border-radius:8px; color:#ddd; display:flex; flex-direction:column; gap:12px; }
input { padding:8px; background:#111; border:1px solid #333; color:#eee; border-radius:4px; }
button { padding:10px; background:#3b82f6; color:white; border:none; border-radius:4px; cursor:pointer; }
.tip { color:#ff8; }
</style>


