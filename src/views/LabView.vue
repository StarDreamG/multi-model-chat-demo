<template>
  <section class="panel gap">
    <h2>Multi-Model Chat Lab</h2>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <ComposerPanel
      :busy="streamLocked"
      :models="models"
      :model-code="activeModelCode"
      @submit="handlePromptSubmit"
      @model-change="handleModelChange"
      @clear="resetStream"
    />
    <MessageStream :entries="eventStream" />
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import ComposerPanel from '../components/ComposerPanel.vue'
import MessageStream from '../components/MessageStream.vue'
import { useConversationStore } from '../stores/useConversationStore'

const conversationStore = useConversationStore()
const { eventStream, streamLocked, models, activeModelCode, errorText } = storeToRefs(conversationStore)
const { submitPrompt, resetStream, changeModel, bootstrapConversation } = conversationStore

onMounted(async () => {
  await bootstrapConversation()
})

const handlePromptSubmit = async (textValue) => {
  await submitPrompt(textValue)
}

const handleModelChange = async (modelCode) => {
  await changeModel(modelCode)
}
</script>
