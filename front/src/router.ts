import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import LoginView from './views/LoginView.vue'
import EditorView from './views/EditorView.vue'
import { useAuthStore } from './stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/editor' },
  { path: '/login', component: LoginView },
  { path: '/editor', component: EditorView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/editor' && !auth.accessToken) {
    return '/login'
  }
})

export default router


