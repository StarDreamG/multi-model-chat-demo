const staticSlices = [
  'This stream is generated locally. ',
  'No private endpoint is configured. ',
  'The module is safe for test-environment sharing.'
]

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// Async generator is used so UI can render chunk-by-chunk output.
export const generateAssistantSlices = async function* (promptText) {
  const normalized = String(promptText || '').trim()
  const chunks = normalized
    ? [`Prompt accepted: ${normalized}. `, ...staticSlices]
    : ['Prompt accepted. ', ...staticSlices]

  for (const chunk of chunks) {
    await sleep(120)
    yield chunk
  }
}
