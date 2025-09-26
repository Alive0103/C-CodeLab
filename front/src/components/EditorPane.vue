<template>
  <div ref="container" style="height:100%; width:100%;"></div>
</template>

<script setup lang="ts">
import * as monaco from 'monaco-editor'
import { onMounted, onBeforeUnmount, ref, watch, defineExpose } from 'vue'

const props = defineProps<{
  modelValue: string
}>()
const emit = defineEmits(['update:modelValue'])

const container = ref<HTMLDivElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  editor = monaco.editor.create(container.value!, {
    value: props.modelValue || '',
    language: 'c',
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 14
  })
  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor!.getValue())
  })
})

onBeforeUnmount(() => {
  editor?.dispose()
})

watch(() => props.modelValue, (v) => {
  if (editor && v !== editor.getValue()) editor.setValue(v || '')
})

function setMarkers(errors: Array<{line: number, column: number, message: string}>) {
  const model = editor?.getModel()
  if (!model) return
  monaco.editor.setModelMarkers(model, 'owner', errors.map(e => ({
    startLineNumber: e.line, startColumn: e.column, endLineNumber: e.line, endColumn: e.column + 1,
    message: e.message, severity: monaco.MarkerSeverity.Error
  })))
}

defineExpose({ getValue: () => editor?.getValue() || '', setMarkers })
</script>

<style scoped>
</style>


