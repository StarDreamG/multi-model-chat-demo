INSERT INTO model_config (
  model_code,
  display_name,
  provider_type,
  endpoint,
  api_key_encrypted,
  timeout_ms,
  max_qps,
  enabled,
  tags
)
VALUES
  (
    'gemma2:2b-code',
    'Gemma2 2B Code',
    'OLLAMA',
    'http://localhost:11434/api/chat',
    NULL,
    60000,
    10,
    0,
    'local,code,plan'
  ),
  (
    'deepseek-coder-v2-instruct',
    'DeepSeek Coder V2 Instruct',
    'OLLAMA',
    'http://localhost:11434/api/chat',
    NULL,
    90000,
    6,
    0,
    'local,code,plan'
  ),
  (
    'deepseek-coder-v2-lite-instruct',
    'DeepSeek Coder V2 Lite Instruct',
    'OLLAMA',
    'http://localhost:11434/api/chat',
    NULL,
    90000,
    8,
    0,
    'local,code,plan'
  ),
  (
    'codellama-7b-code-instruct',
    'CodeLlama 7B Code Instruct',
    'OLLAMA',
    'http://localhost:11434/api/chat',
    NULL,
    90000,
    8,
    0,
    'local,code,plan'
  )
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  provider_type = VALUES(provider_type),
  endpoint = VALUES(endpoint),
  timeout_ms = VALUES(timeout_ms),
  max_qps = VALUES(max_qps),
  tags = VALUES(tags);
