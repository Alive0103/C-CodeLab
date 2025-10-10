import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import EditorView from './views/EditorView.vue'
import CodeHistoryView from './views/CodeHistoryView.vue'
import UserProfileView from './views/UserProfileView.vue'
import { useAuthStore } from './stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/editor' },
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  { path: '/editor', component: EditorView },
  { path: '/code-history', component: CodeHistoryView },
  { path: '/profile', component: UserProfileView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  // 需要认证的页面列表
  const protectedRoutes = ['/editor', '/code-history', '/profile']
  
  if (protectedRoutes.includes(to.path)) {
    const auth = useAuthStore()
    // 如果store中没有用户信息，尝试获取
    if (!auth.user) {
      try {
        const { getUser } = await import('./api/auth')
        const res = await getUser()
        if (res.data.data) {
          auth.setUser(res.data.data)
        } else {
          return '/login'
        }
      } catch (err) {
        console.error('获取用户信息失败:', err)
        return '/login'
      }
    }
  }
  
  // 如果访问登录或注册页面，清除可能的错误状态
  if (to.path === '/login' || to.path === '/register') {
    // 不需要特殊处理，让页面正常加载
  }
})

export default router