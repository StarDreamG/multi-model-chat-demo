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
  provider_type VARCHAR(32) NOT NULL,
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
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  model_code VARCHAR(64) NULL,
  token_in INT NULL,
  token_out INT NULL,
  latency_ms INT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
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
  status VARCHAR(16) NOT NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_calllog_model_created (model_code, created_at DESC),
  KEY idx_calllog_request (request_id)
) ENGINE=InnoDB;

CREATE INDEX idx_session_user_updated ON chat_session (user_id, updated_at DESC);
CREATE INDEX idx_message_session_created ON chat_message (session_id, created_at ASC);
