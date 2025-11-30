<template>
  <div class="code-history">
    <div class="header">
      <h1>执行记录</h1>
    </div>

    <!-- 执行记录列表 -->
    <div class="content">
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
import { getMyExecutionRecords, deleteExecutionRecord } from '../api/user'

const executions = ref([])
const executionsLoading = ref(false)

onMounted(() => {
  loadExecutions()
})

async function loadExecutions() {
  if (executionsLoading.value) return // 防止重复加载
  executionsLoading.value = true
  try {
    const res = await getMyExecutionRecords()
    // 后端返回的是 Page 对象，需要取 content 字段
    executions.value = res.data.data?.content || []
  } catch (error) {
    console.error('加载执行记录失败:', error)
    executions.value = []
  } finally {
    executionsLoading.value = false
  }
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleString('zh-CN')
}

function viewExecution(execution: any) {
  // 跳转到编辑器并加载执行记录
  window.location.href = `/editor?execution=${execution.id}`
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

.content {
  min-height: 400px;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #888;
}

.executions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.execution-item {
  background: #222;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.execution-info {
  flex: 1;
  margin-right: 20px;
}

.execution-info h3 {
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
