<template>
  <form class="composer" @submit.prevent="emitSubmit">
    <select
      class="composer-select"
      :disabled="busy || !models.length"
      :value="modelCode"
      @change="emitModelChange"
    >
      <option v-for="model in models" :key="model.modelCode" :value="model.modelCode">
        {{ model.displayName }}
      </option>
    </select>

    <input
      v-model="inputBuffer"
      class="composer-input"
      :disabled="busy"
      placeholder="Type your message"
    />

    <button type="submit" :disabled="busy || !inputBuffer.trim()">{{ busy ? 'Streaming...' : 'Send' }}</button>
    <button type="button" class="secondary" :disabled="busy" @click="$emit('clear')">Clear</button>
  </form>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  busy: {
    type: Boolean,
    default: false
  },
  models: {
    type: Array,
    default: () => []
  },
  modelCode: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['submit', 'clear', 'model-change'])
const inputBuffer = ref('')

const emitSubmit = () => {
  const payload = inputBuffer.value.trim()
  if (!payload || props.busy) return
  emit('submit', payload)
  inputBuffer.value = ''
}

const emitModelChange = (event) => {
  emit('model-change', event.target.value)
}
</script>
