<template>
  <section class="message-board">
    <article
      v-for="entry in entries"
      :key="entry.id"
      :class="['bubble', entry.role === 'user' ? 'bubble-user' : 'bubble-assistant']"
    >
      <strong>{{ entry.role === 'user' ? 'User' : 'Assistant' }}:</strong>
      <span>{{ entry.text }}</span>
      <small v-if="entry.modelCode" class="meta">model={{ entry.modelCode }}</small>
    </article>
    <p v-if="!entries.length" class="empty-tip">Start a chat after backend is ready.</p>
  </section>
</template>

<script setup>
defineProps({
  entries: {
    type: Array,
    default: () => []
  }
})
</script>
