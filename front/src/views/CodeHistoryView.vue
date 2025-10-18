<template>
  <div class="code-history">
    <div class="header">
      <h1>我的代码历史</h1>
      <div class="tabs">
        <button 
          :class="{ active: activeTab === 'snippets' }" 
          @click="activeTab = 'snippets'"
        >
          代码片段
        </button>
        <button 
          :class="{ active: activeTab === 'executions' }" 
          @click="activeTab = 'executions'"
        >
          执行记录
        </button>
      </div>
    </div>

    <!-- 代码片段列表 -->
    <div v-if="activeTab === 'snippets'" class="content">
      <div v-if="snippetsLoading" class="loading">加载中...</div>
      <div v-else-if="snippets.length === 0" class="empty">暂无代码片段</div>
      <div v-else class="snippets-list">
        <div 
          v-for="snippet in snippets" 
          :key="snippet.id" 
          class="snippet-item"
        >
          <div class="snippet-info">
            <h3>{{ snippet.title }}</h3>
            <p class="meta">
              {{ snippet.language }} | 
              {{ formatDate(snippet.createdAt) }} |
              {{ snippet.isPublic ? '公开' : '私有' }}
            </p>
            <div class="code-preview">{{ snippet.codeContent.substring(0, 100) }}...</div>
          </div>
          <div class="actions">
            <button @click="viewSnippet(snippet)" class="btn-primary">查看</button>
            <button @click="deleteSnippet(snippet.id)" class="btn-danger">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 执行记录列表 -->
    <div v-if="activeTab === 'executions'" class="content">
      <div v-if="executionsLoading" class="loading">加载中...</div>
      <div v-else-if="executions.length === 0" class="empty">暂无执行记录</div>
      <div v-else class="executions-list">
        <div 
          v-for="execution in executions" 
          :key="execution.id" 
          class="execution-item"
        >
          <div class="execution-info">
            <h3>{{ execution.title || '未命名执行' }}</h3>
            <p class="meta">
              {{ formatDate(execution.createdAt) }} |
              退出码: {{ execution.exitCode }}
            </p>
            <div class="code-preview">{{ execution.code.substring(0, 100) }}...</div>
            <div v-if="execution.output" class="output-preview">
              <strong>输出:</strong> {{ execution.output.substring(0, 50) }}...
            </div>
            <div v-if="execution.error" class="error-preview">
              <strong>错误:</strong> {{ execution.error.substring(0, 50) }}...
            </div>
          </div>
          <div class="actions">
            <button @click="viewExecution(execution)" class="btn-primary">查看</button>
            <button @click="deleteExecution(execution.id)" class="btn-danger">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyCodeSnippets, getMyExecutionRecords, deleteCodeSnippet, deleteExecutionRecord } from '../api/user'

const activeTab = ref<'snippets' | 'executions'>('snippets')
const snippets = ref([])
const executions = ref([])
const snippetsLoading = ref(false)
const executionsLoading = ref(false)

onMounted(() => {
  loadSnippets()
})

async function loadSnippets() {
  snippetsLoading.value = true
  try {
    const res = await getMyCodeSnippets()
    snippets.value = res.data.data
  } catch (error) {
    console.error('加载代码片段失败:', error)
  } finally {
    snippetsLoading.value = false
  }
}

async function loadExecutions() {
  executionsLoading.value = true
  try {
    const res = await getMyExecutionRecords()
    executions.value = res.data.data.content || []
  } catch (error) {
    console.error('加载执行记录失败:', error)
  } finally {
    executionsLoading.value = false
  }
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleString('zh-CN')
}

function viewSnippet(snippet: any) {
  // 跳转到编辑器并加载代码片段
  window.location.href = `/editor?snippet=${snippet.id}`
}

function viewExecution(execution: any) {
  // 跳转到编辑器并加载执行记录
  window.location.href = `/editor?execution=${execution.id}`
}

async function deleteSnippet(id: number) {
  if (confirm('确定要删除这个代码片段吗？')) {
    try {
      await deleteCodeSnippet(id)
      snippets.value = snippets.value.filter(s => s.id !== id)
    } catch (error) {
      console.error('删除失败:', error)
      alert('删除失败')
    }
  }
}

async function deleteExecution(id: number) {
  if (confirm('确定要删除这个执行记录吗？')) {
    try {
      await deleteExecutionRecord(id)
      executions.value = executions.value.filter(e => e.id !== id)
    } catch (error) {
      console.error('删除失败:', error)
      alert('删除失败')
    }
  }
}

// 切换标签时加载对应数据
function switchTab(tab: 'snippets' | 'executions') {
  activeTab.value = tab
  if (tab === 'executions' && executions.value.length === 0) {
    loadExecutions()
  }
}
</script>

<style scoped>
.code-history {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #1a1a1a;
  min-height: 100vh;
  color: #ddd;
}

.header {
  margin-bottom: 30px;
}

.header h1 {
  color: #fff;
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  gap: 10px;
}

.tabs button {
  padding: 10px 20px;
  background: #333;
  color: #ddd;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.tabs button.active {
  background: #3b82f6;
  color: white;
}

.tabs button:hover {
  background: #444;
}

.tabs button.active:hover {
  background: #2563eb;
}

.content {
  min-height: 400px;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #888;
}

.snippets-list, .executions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.snippet-item, .execution-item {
  background: #222;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.snippet-info, .execution-info {
  flex: 1;
  margin-right: 20px;
}

.snippet-info h3, .execution-info h3 {
  color: #fff;
  margin: 0 0 10px 0;
  font-size: 18px;
}

.meta {
  color: #888;
  font-size: 14px;
  margin: 5px 0;
}

.code-preview {
  background: #111;
  padding: 10px;
  border-radius: 4px;
  margin: 10px 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #ccc;
  border-left: 3px solid #3b82f6;
}

.output-preview, .error-preview {
  margin: 10px 0;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
}

.output-preview {
  background: #0f3d0f;
  color: #90ee90;
}

.error-preview {
  background: #3d0f0f;
  color: #ff6b6b;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-primary, .btn-danger {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover {
  background: #b91c1c;
}
</style>
