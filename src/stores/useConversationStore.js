import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createSession,
  listMessages,
  listModels,
  listSessions,
  streamChatCompletion,
  updateSession
} from '../services/chatGateway'

export const useConversationStore = defineStore('conversation_store', () => {
  const models = ref([])
  const sessions = ref([])
  const currentSessionId = ref(null)
  const activeModelCode = ref('')
  const eventStream = ref([])
  const streamLocked = ref(false)
  const bootstrapped = ref(false)
  const errorText = ref('')

  const toStreamEntry = (message) => ({
    id: `${message.role}-${message.id || Date.now()}`,
    role: message.role,
    text: message.content,
    modelCode: message.modelCode || activeModelCode.value
  })

  const hydrateMessages = async () => {
    if (!currentSessionId.value) return
    const rows = await listMessages(currentSessionId.value)
    eventStream.value = rows.map(toStreamEntry)
  }

  const bootstrapConversation = async () => {
    if (bootstrapped.value) return

    errorText.value = ''
    models.value = await listModels()
    activeModelCode.value = models.value[0]?.modelCode || ''

    const page = await listSessions()
    sessions.value = page.items || []

    if (sessions.value.length) {
      currentSessionId.value = sessions.value[0].id
      activeModelCode.value = sessions.value[0].modelCode || activeModelCode.value
      await hydrateMessages()
    } else if (activeModelCode.value) {
      const session = await createSession({
        title: 'New Chat',
        modelCode: activeModelCode.value
      })
      sessions.value = [session]
      currentSessionId.value = session.id
      eventStream.value = []
    }

    bootstrapped.value = true
  }

  const pushUserItem = (textValue) => {
    eventStream.value.push({
      id: `u-${Date.now()}`,
      role: 'user',
      text: textValue,
      modelCode: activeModelCode.value
    })
  }

  const pushAssistantDraft = () => {
    const draftItem = {
      id: `a-${Date.now()}`,
      role: 'assistant',
      text: '',
      modelCode: activeModelCode.value
    }
    eventStream.value.push(draftItem)
    return draftItem
  }

  const submitPrompt = async (rawText) => {
    const inputText = String(rawText || '').trim()
    if (!inputText || streamLocked.value) return

    await bootstrapConversation()
    if (!currentSessionId.value || !activeModelCode.value) {
      errorText.value = 'No available session/model. Please check backend status.'
      return
    }

    pushUserItem(inputText)
    const assistantDraft = pushAssistantDraft()
    streamLocked.value = true
    errorText.value = ''

    try {
      const messagePayload = eventStream.value
        .filter((item) => item.role === 'user' || item.role === 'assistant')
        .map((item) => ({ role: item.role, content: item.text }))

      await streamChatCompletion(
        {
          sessionId: currentSessionId.value,
          modelCode: activeModelCode.value,
          messages: messagePayload,
          temperature: 0.7,
          topP: 0.9,
          maxTokens: 1024
        },
        {
          onDelta: (slice) => {
            assistantDraft.text += slice
          }
        }
      )

      await hydrateMessages()
    } catch (error) {
      errorText.value = error?.message || 'Stream failed'
      assistantDraft.text = '[Error] Failed to get assistant response.'
    } finally {
      streamLocked.value = false
    }
  }

  const changeModel = async (modelCode) => {
    if (!modelCode || !currentSessionId.value) return
    activeModelCode.value = modelCode

    const updated = await updateSession(currentSessionId.value, { modelCode })
    sessions.value = sessions.value.map((item) => (item.id === updated.id ? updated : item))
  }

  const resetStream = () => {
    eventStream.value = []
  }

  return {
    models,
    sessions,
    currentSessionId,
    activeModelCode,
    eventStream,
    streamLocked,
    bootstrapped,
    errorText,
    bootstrapConversation,
    submitPrompt,
    changeModel,
    resetStream
  }
})
