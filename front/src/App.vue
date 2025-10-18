<template>
  <router-view />
  <AuthErrorHandler />
</template>

<script setup lang="ts">
import AuthErrorHandler from './components/AuthErrorHandler.vue'
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
</style>


