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
    'deepseek-coder:1.3b-instruct',
    'DeepSeek Coder 1.3B Instruct',
    'OLLAMA',
    'http://localhost:11434/api/chat',
    NULL,
    90000,
    8,
    1,
    'local,code,deployed,fallback'
  )
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  provider_type = VALUES(provider_type),
  endpoint = VALUES(endpoint),
  timeout_ms = VALUES(timeout_ms),
  max_qps = VALUES(max_qps),
  enabled = VALUES(enabled),
  tags = VALUES(tags);
