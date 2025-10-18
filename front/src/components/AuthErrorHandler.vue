<template>
  <div v-if="showError" class="auth-error-overlay">
    <div class="auth-error-modal">
      <div class="error-icon">⚠️</div>
      <h3>认证失败</h3>
      <p>{{ errorMessage }}</p>
      <div class="error-actions">
        <button @click="retry" class="retry-btn">重试</button>
        <button @click="goToLogin" class="login-btn">重新登录</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const showError = ref(false)
const errorMessage = ref('')

// 监听认证错误事件
function handleAuthError(event: CustomEvent) {
  errorMessage.value = event.detail.message || '认证失败，请重新登录'
  showError.value = true
}

function retry() {
  showError.value = false
  // 刷新页面重试
  window.location.reload()
}

function goToLogin() {
  showError.value = false
  auth.clear()
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  window.addEventListener('auth-error', handleAuthError as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('auth-error', handleAuthError as EventListener)
})
</script>

<style scoped>
.auth-error-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.auth-error-modal {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 24px;
  max-width: 400px;
  text-align: center;
  color: #ddd;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.auth-error-modal h3 {
  margin: 0 0 12px 0;
  color: #ff6b6b;
}

.auth-error-modal p {
  margin: 0 0 20px 0;
  color: #aaa;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.retry-btn, .login-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.retry-btn {
  background: #3b82f6;
  color: white;
}

.login-btn {
  background: #ef4444;
  color: white;
}

.retry-btn:hover {
  background: #2563eb;
}

.login-btn:hover {
  background: #dc2626;
}
</style>
