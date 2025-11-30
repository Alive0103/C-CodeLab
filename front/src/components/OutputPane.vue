<template>
  <div class="output">
    <div class="header">执行结果</div>
    <div class="content">
      <div class="status-section">
        <div class="status-item">
          <span class="label">退出码:</span>
          <span class="value">{{ result?.exitCode ?? '-' }}</span>
        </div>
        <div class="status-item">
          <span class="label">执行状态:</span>
          <span :class="['value', result?.success ? 'success' : 'failed']">
            {{ result?.success ? '成功' : '失败' }}
          </span>
        </div>
      </div>
      
      <div v-if="result?.output" class="output-section">
        <div class="section-title">输出内容:</div>
        <pre class="output-text">{{ result.output }}</pre>
      </div>
      
      <div v-if="result?.error" class="error-section">
        <div class="section-title">错误信息:</div>
        <pre class="error-text">{{ result.error }}</pre>
      </div>
      
      <div v-if="!result" class="empty-state">
        暂无执行结果
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ result?: { success: boolean, output: string, error: string, exitCode: number } }>()
</script>

<style scoped>
.output { 
  height: 100%; 
  display: flex; 
  flex-direction: column; 
  background: #1a1a1a;
  border-left: 1px solid #333;
}

.header {
  padding: 12px 16px;
  font-weight: bold;
  font-size: 16px;
  background: #222;
  color: #fff;
  border-bottom: 1px solid #333;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.status-section {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #333;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  color: #888;
  font-size: 12px;
}

.value {
  color: #ddd;
  font-size: 16px;
  font-weight: 500;
}

.value.success {
  color: #4ade80;
}

.value.failed {
  color: #f87171;
}

.output-section, .error-section {
  margin-bottom: 20px;
}

.section-title {
  color: #ccc;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.output-text, .error-text {
  background: #111;
  color: #ddd;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  border: 1px solid #333;
  margin: 0;
}

.error-text {
  color: #ff8888;
  background: #2a1a1a;
  border-color: #4a2a2a;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #888;
  font-size: 14px;
}
</style>


