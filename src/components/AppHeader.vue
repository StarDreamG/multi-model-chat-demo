<template>
  <header class="topbar">
    <div class="brand-block">Personal Open Demo</div>
    <nav class="nav-links">
      <RouterLink to="/">Home</RouterLink>
      <RouterLink to="/lab">Lab</RouterLink>
      <RouterLink to="/docs">Docs</RouterLink>
    </nav>
  </header>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
