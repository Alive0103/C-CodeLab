import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/index.css'
import { useAuthStore } from './stores/auth'
import { http } from './api/http'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 初始化时检查token并获取用户信息
const auth = useAuthStore()
const token = localStorage.getItem('token')
if (token) {
  // 设置token到axios默认header
  http.defaults.headers.common['Authorization'] = token
  // 获取用户信息
  http.get('/user').then(res => {
    auth.setUser(res.data.data)
  }).catch(() => {
    // token无效，清除
    localStorage.removeItem('token')
    delete http.defaults.headers.common['Authorization']
  })
}

app.mount('#app')


