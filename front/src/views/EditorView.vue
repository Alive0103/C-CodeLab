<template>
  <div class="page">
    <div class="topbar">
      <input v-model="title" placeholder="标题（可选）" />
      <div class="spacer"></div>
      <button @click="onSave" :disabled="busy">保存</button>
      <button @click="onRun" :disabled="busy">{{ busy ? '运行中...' : '运行' }}</button>
    </div>
    <div class="main">
      <div class="left">
        <EditorPane v-model="code" ref="editor" />
      </div>
      <div class="right">
        <OutputPane :result="result" />
        <HistoryList :items="history" @select="applyHistory" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import EditorPane from '../components/EditorPane.vue'
import OutputPane from '../components/OutputPane.vue'
import HistoryList from '../components/HistoryList.vue'
import { saveSnippet, runCode, listResults } from '../api/code'
import { createExecutionWS } from '../utils/websocket'

const code = ref("#include <stdio.h>\nint main(){ printf(\"Hello, World!\\n\"); return 0; }")
const title = ref('Hello World')
const busy = ref(false)
const result = ref<{success:boolean, output:string, error:string, exitCode:number} | undefined>()
const history = ref<Array<{id:number,title:string,exitCode:number,createdAt:string}>>([])

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
  loadHistory()
  createExecutionWS((data) => {
    if (data && typeof data.success === 'boolean') {
      result.value = data
    }
  })
})
</script>

<style scoped>
.page { display:flex; flex-direction:column; height:100vh; background:#0b0b0b; color:#ddd; }
.topbar { display:flex; align-items:center; gap:8px; padding:8px; background:#151515; border-bottom:1px solid #222; }
.spacer { flex:1; }
input { padding:6px 8px; background:#0f0f0f; border:1px solid #333; color:#eee; border-radius:4px; min-width: 240px; }
button { padding:8px 12px; background:#3b82f6; color:white; border:none; border-radius:4px; cursor:pointer; }
.main { flex:1; display:flex; min-height:0; }
.left { flex: 2; min-width: 0; }
.right { flex: 1; display:flex; flex-direction:column; min-width: 0; }
.right > * { flex:1; min-height:0; }
</style>


