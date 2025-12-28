<template>
  <div class="page">
    <div class="topbar">
      <input v-model="title" placeholder="标题（可选）" />
      <div class="spacer"></div>
      <button @click="goToCodeHistory" class="nav-btn">代码历史</button>
      <button @click="goToProfile" class="nav-btn">个人中心</button>
      <button @click="handleLogout" class="nav-btn logout-btn">登出</button>
      <button @click="onSave" :disabled="busy">保存</button>
      <button @click="onRun" :disabled="busy">{{ busy ? '运行中...' : '运行' }}</button>
    </div>
    <div class="main">
      <div class="left" :style="{ width: leftWidth + 'px' }">
        <EditorPane v-model="code" ref="editor" />
      </div>
      <div 
        class="resizer" 
        @mousedown="startResize"
        :class="{ resizing: isResizing }"
      ></div>
      <div class="right" :style="{ width: rightWidth + 'px' }">
        <OutputPane :result="result" />
        <HistoryList :items="history" @select="applyHistory" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import EditorPane from '../components/EditorPane.vue'
import OutputPane from '../components/OutputPane.vue'
import HistoryList from '../components/HistoryList.vue'
import { saveSnippet, runCode, listResults } from '../api/code'
import { createExecutionWS } from '../utils/websocket'

const router = useRouter()
const auth = useAuthStore()

const code = ref("#include <stdio.h>\nint main(){ printf(\"Hello, World!\\n\"); return 0; }")
const title = ref('Hello World')
const busy = ref(false)
const result = ref<{success:boolean, output:string, error:string, exitCode:number} | undefined>()
const history = ref<Array<{id:number,title:string,exitCode:number,createdAt:string}>>([])

// 分隔条相关状态
const isResizing = ref(false)
const leftWidth = ref(0)
const rightWidth = ref(0)
const resizerWidth = 4 // 分隔条宽度

// 初始化面板宽度
function initPanelWidths() {
  const savedLeftWidth = localStorage.getItem('editor-left-width')
  const savedRightWidth = localStorage.getItem('editor-right-width')
  
  if (savedLeftWidth && savedRightWidth) {
    const savedLeft = parseInt(savedLeftWidth)
    const savedRight = parseInt(savedRightWidth)
    const totalWidth = window.innerWidth
    
    // 验证保存的值是否有效
    if (savedLeft >= 300 && savedRight >= 300 && (savedLeft + savedRight + resizerWidth) <= totalWidth) {
      leftWidth.value = savedLeft
      rightWidth.value = savedRight
      return
    }
  }
  
  // 默认比例：左侧占 60%，右侧占 40%
  const totalWidth = window.innerWidth
  leftWidth.value = Math.floor(totalWidth * 0.6)
  rightWidth.value = totalWidth - leftWidth.value - resizerWidth
}

// 开始调整大小
function startResize(e: MouseEvent) {
  isResizing.value = true
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  // 防止拖拽时选中文本
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
  e.preventDefault()
}

// 处理调整大小
function handleResize(e: MouseEvent) {
  if (!isResizing.value) return
  
  const totalWidth = window.innerWidth
  const newLeftWidth = e.clientX
  const minLeftWidth = 300 // 左侧最小宽度
  const minRightWidth = 300 // 右侧最小宽度
  
  if (newLeftWidth >= minLeftWidth && (totalWidth - newLeftWidth - resizerWidth) >= minRightWidth) {
    leftWidth.value = newLeftWidth
    rightWidth.value = totalWidth - newLeftWidth - resizerWidth
  }
}

// 停止调整大小
function stopResize() {
  isResizing.value = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  // 恢复文本选择
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
  
  // 保存宽度到 localStorage
  localStorage.setItem('editor-left-width', leftWidth.value.toString())
  localStorage.setItem('editor-right-width', rightWidth.value.toString())
}

// 窗口大小改变时调整
function handleResizeWindow() {
  if (!isResizing.value && leftWidth.value > 0 && rightWidth.value > 0) {
    const totalWidth = window.innerWidth
    const currentTotal = leftWidth.value + rightWidth.value + resizerWidth
    if (currentTotal > 0) {
      const ratio = leftWidth.value / currentTotal
      const newLeftWidth = Math.floor(totalWidth * ratio)
      const newRightWidth = totalWidth - newLeftWidth - resizerWidth
      
      // 确保最小宽度
      if (newLeftWidth >= 300 && newRightWidth >= 300) {
        leftWidth.value = newLeftWidth
        rightWidth.value = newRightWidth
      }
    }
  }
}

async function onSave() {
  await saveSnippet({ title: title.value || 'untitled', codeContent: code.value, language: 'c', isPublic: false })
  await loadHistory()
}

async function onRun() {
  if (busy.value) return
  busy.value = true
  try {
    const res = await runCode({ code: code.value, title: title.value })
    const data = res.data.data
    if (data?.success !== undefined) result.value = data
  } finally {
    busy.value = false
  }
}

async function loadHistory() {
  const res = await listResults()
  history.value = res.data.data || []
}

function applyHistory(item: any) {
  title.value = item.title
}

onMounted(() => {
  initPanelWidths()
  loadHistory()
  createExecutionWS((data) => {
    if (data && typeof data.success === 'boolean') {
      result.value = data
    }
  })
  window.addEventListener('resize', handleResizeWindow)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResizeWindow)
  stopResize()
})

function goToCodeHistory() {
  router.push('/code-history')
}

function goToProfile() {
  router.push('/profile')
}

async function handleLogout() {
  if (confirm('确定要登出吗？')) {
    await auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.page { display:flex; flex-direction:column; height:100vh; background:#0b0b0b; color:#ddd; }
.topbar { display:flex; align-items:center; gap:8px; padding:8px; background:#151515; border-bottom:1px solid #222; }
.spacer { flex:1; }
input { padding:6px 8px; background:#0f0f0f; border:1px solid #333; color:#eee; border-radius:4px; min-width: 240px; }
button { padding:8px 12px; background:#3b82f6; color:white; border:none; border-radius:4px; cursor:pointer; }
.nav-btn { background:#444; margin-right:8px; }
.nav-btn:hover { background:#555; }
.logout-btn { background:#dc2626; }
.logout-btn:hover { background:#b91c1c; }
.main { flex:1; display:flex; min-height:0; position: relative; }
.left { min-width: 300px; max-width: calc(100% - 300px); flex-shrink: 0; }
.right { min-width: 300px; max-width: calc(100% - 300px); flex-shrink: 0; display:flex; flex-direction:column; }
.right > * { flex:1; min-height:0; }

.resizer {
  width: 4px;
  background: #333;
  cursor: col-resize;
  flex-shrink: 0;
  position: relative;
  transition: background 0.2s;
}

.resizer:hover {
  background: #3b82f6;
}

.resizer.resizing {
  background: #3b82f6;
}

.resizer::before {
  content: '';
  position: absolute;
  left: -2px;
  right: -2px;
  top: 0;
  bottom: 0;
  cursor: col-resize;
}
</style>


