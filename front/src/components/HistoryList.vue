<template>
  <div class="history">
    <div class="header">执行记录</div>
    <div v-for="item in items" :key="item.id" class="item" @click="$emit('select', item)">
      <div class="title">{{ item.title || '(无标题)' }}</div>
      <div class="meta">{{ item.createdAt }}</div>
      <div class="exit">exit={{ item.exitCode }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ items: Array<{ id: number, title: string, exitCode: number, createdAt: string }> }>()
defineEmits(['select'])
</script>

<style scoped>
.history { height: 100%; overflow: auto; border-left: 1px solid #333; }
.header { padding: 8px; font-weight: bold; background: #222; color: #ccc; }
.item { padding: 8px; border-bottom: 1px solid #333; cursor: pointer; }
.item:hover { background: #1b1b1b; }
.title { color: #eee; }
.meta { color: #888; font-size: 12px; }
.exit { color: #aaa; font-size: 12px; }
</style>


