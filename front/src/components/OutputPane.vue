<template>
  <div class="output">
    <div class="row"><b>Exit Code:</b> {{ result?.exitCode ?? '-' }}</div>
    <div class="row"><b>Success:</b> {{ result?.success ? 'Yes' : 'No' }}</div>
    <div class="row"><b>Output:</b></div>
    <pre>{{ result?.output || '' }}</pre>
    <div v-if="result?.error">
      <div class="row"><b>Error:</b></div>
      <pre class="err">{{ result?.error }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ result?: { success: boolean, output: string, error: string, exitCode: number } }>()
</script>

<style scoped>
.output { height: 100%; display: flex; flex-direction: column; }
.row { margin: 6px 0; }
pre { background: #111; color: #ddd; padding: 8px; white-space: pre-wrap; word-break: break-word; }
pre.err { color: #ff8888; }
</style>


