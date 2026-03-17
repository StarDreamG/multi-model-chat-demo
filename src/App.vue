<template>
  <div class="root-shell">
    <AppHeader />
    <main class="page-stage">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
</script>
