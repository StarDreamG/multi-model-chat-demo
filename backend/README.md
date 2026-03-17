# Multi-Model Chat Backend (Spring Boot)

## Stack

- Java 17
- Spring Boot 3.3
- Spring Security (JWT)
- Spring Data JPA
- Flyway
- MySQL 8

## Features (Current)

- JWT login/auth (`/api/auth/login`, `/api/auth/me`)
- Session/message persistence in MySQL
- Model config persistence in MySQL
- Provider adapter gateway:
  - `OPENAI_COMPATIBLE`
  - `OLLAMA`
  - `VLLM`
  - `CUSTOM_HTTP` (OpenAI-compatible payload)
- Admin metrics from `model_call_log`

## Prerequisites

- JDK 17+
- Maven 3.9+
- MySQL 8+

## Env (examples)

- `DB_URL=jdbc:mysql://localhost:3306/multi_model_chat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `JWT_SECRET=replace-this-with-at-least-32-characters-secret-key`
- `JWT_EXPIRE_SECONDS=86400`

## Run

```bash
cd backend
mvn spring-boot:run
```

Flyway will auto-create schema on startup.

## Default Admin

- username: `admin`
- password: `admin123`

## Important Note

`apiKey` is currently stored directly in `api_key_encrypted` for MVP speed.
Replace with real encryption/KMS before production.