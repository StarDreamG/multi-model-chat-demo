-- Multi-model chat app schema (MVP)
-- Target: MySQL 8+

CREATE DATABASE IF NOT EXISTS multi_model_chat
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE multi_model_chat;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  model_code VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  provider_type VARCHAR(32) NOT NULL COMMENT 'OPENAI_COMPATIBLE/OLLAMA/VLLM/CUSTOM_HTTP',
  endpoint VARCHAR(512) NOT NULL,
  api_key_encrypted VARCHAR(1024) NULL,
  timeout_ms INT NOT NULL DEFAULT 30000,
  max_qps INT NOT NULL DEFAULT 20,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  tags VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  model_code VARCHAR(64) NOT NULL,
  archived TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL COMMENT 'system/user/assistant',
  content MEDIUMTEXT NOT NULL,
  model_code VARCHAR(64) NULL,
  token_in INT NULL,
  token_out INT NULL,
  latency_ms INT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/ERROR/STOPPED',
  error_code VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS model_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id VARCHAR(64) NOT NULL,
  user_id BIGINT NULL,
  session_id BIGINT NULL,
  model_code VARCHAR(64) NOT NULL,
  request_tokens INT NULL,
  response_tokens INT NULL,
  duration_ms INT NOT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'SUCCESS/ERROR/TIMEOUT/RATE_LIMIT',
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_calllog_model_created (model_code, created_at DESC),
  KEY idx_calllog_request (request_id)
) ENGINE=InnoDB;

CREATE INDEX idx_session_user_updated
  ON chat_session (user_id, updated_at DESC);

CREATE INDEX idx_message_session_created
  ON chat_message (session_id, created_at ASC);

CREATE INDEX idx_message_session_role
  ON chat_message (session_id, role);

INSERT INTO sys_role (role_code, role_name)
VALUES ('ADMIN', 'Administrator'), ('USER', 'Normal User')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO model_config (model_code, display_name, provider_type, endpoint, timeout_ms, max_qps, enabled, tags)
VALUES
('gpt-4o-mini', 'GPT-4o Mini', 'OPENAI_COMPATIBLE', 'https://api.example.com/v1/chat/completions', 30000, 30, 1, 'general,fast'),
('qwen-max', 'Qwen Max', 'OPENAI_COMPATIBLE', 'https://api.example.com/v1/chat/completions', 40000, 20, 1, 'general,strong'),
('local-llama3', 'Local Llama 3', 'OLLAMA', 'http://localhost:11434/api/chat', 60000, 10, 1, 'local,private')
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  provider_type = VALUES(provider_type),
  endpoint = VALUES(endpoint),
  timeout_ms = VALUES(timeout_ms),
  max_qps = VALUES(max_qps),
  enabled = VALUES(enabled),
  tags = VALUES(tags);
