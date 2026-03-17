import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const DEMO_USERNAME = import.meta.env.VITE_DEMO_USERNAME || 'admin'
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD || 'admin123'

let authToken = ''

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000
})

http.interceptors.request.use((config) => {
  if (authToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

const login = async () => {
  const { data } = await http.post('/api/auth/login', {
    username: DEMO_USERNAME,
    password: DEMO_PASSWORD
  })
  authToken = data.token
  return data
}

const ensureAuth = async () => {
  if (authToken) return authToken
  await login()
  return authToken
}

export const listModels = async () => {
  await ensureAuth()
  const { data } = await http.get('/api/models')
  return data
}

export const listSessions = async () => {
  await ensureAuth()
  const { data } = await http.get('/api/sessions', { params: { page: 1, pageSize: 20 } })
  return data
}

export const createSession = async (payload) => {
  await ensureAuth()
  const { data } = await http.post('/api/sessions', payload)
  return data
}

export const updateSession = async (sessionId, payload) => {
  await ensureAuth()
  const { data } = await http.patch(`/api/sessions/${sessionId}`, payload)
  return data
}

export const listMessages = async (sessionId) => {
  await ensureAuth()
  const { data } = await http.get(`/api/sessions/${sessionId}/messages`)
  return data
}

const parseSseEvent = (blockText) => {
  const lines = blockText.split('\n')
  let eventName = 'message'
  const dataLines = []

  for (const line of lines) {
    if (!line) continue
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  return { eventName, data: dataLines.join('\n') }
}

export const streamChatCompletion = async (payload, hooks = {}) => {
  const token = await ensureAuth()

  const response = await fetch(`${API_BASE_URL}/api/chat/completions/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(payload)
  })

  if (!response.ok || !response.body) {
    throw new Error(`stream request failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''

    for (const block of blocks) {
      const { eventName, data } = parseSseEvent(block)
      if (eventName === 'start' && hooks.onStart) hooks.onStart(data)
      if (eventName === 'delta' && hooks.onDelta) hooks.onDelta(data)
      if (eventName === 'done' && hooks.onDone) hooks.onDone(data)
    }
  }

  if (buffer.trim()) {
    const { eventName, data } = parseSseEvent(buffer)
    if (eventName === 'delta' && hooks.onDelta) hooks.onDelta(data)
    if (eventName === 'done' && hooks.onDone) hooks.onDone(data)
  }
}