# Multi-Model Chat Web Demo

A frontend-backend separated web application starter:

- Frontend: Vue 3 + Vite + Pinia
- Backend: Java 17 + Spring Boot + MySQL + JWT
- Goal: deploy multiple models and switch model in chat UI

## Project Structure

- `src/`: Vue frontend
- `backend/`: Spring Boot backend
- `docs/db-schema-mvp.sql`: MySQL schema design
- `docs/openapi-mvp.yaml`: API contract (OpenAPI 3.0)
- `docs/multi-model-chat-prd-and-functional-design.md`: PRD and functional design
- `docs/coding-model-pull-plan.md`: coding model deployment plan
- `docs/project-requirements-and-phase2-plan-2026-03-18.md`: project requirements, constraints, goals and phase-2 roadmap

## Local Run

### 1) MySQL

Create database:

```sql
CREATE DATABASE IF NOT EXISTS multi_model_chat DEFAULT CHARACTER SET utf8mb4;
```

### 2) Backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

### 3) Frontend

```bash
npm install
npm run dev
```

Frontend URL (default): `http://localhost:5173`

## Frontend Env

Copy `.env.example` to `.env.local` and adjust values if needed.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_DEMO_USERNAME=admin
VITE_DEMO_PASSWORD=admin123
```

## Backend Capability (Current)

- MySQL persistence for user/session/message/model config/call logs
- JWT authentication and role guard for `/api/admin/**`
- Multi-provider adapter invocation:
  - `OPENAI_COMPATIBLE`
  - `OLLAMA`
  - `VLLM`
  - `CUSTOM_HTTP`
- Streaming response endpoint (`/api/chat/completions/stream`)

## Notes

Current stream endpoint uses backend SSE chunking based on final provider response.
If you need true upstream token streaming, next iteration can bridge provider stream directly.

## Storage Suggestion

- Default source workspace: `D:\aFatGuyCode`.
- If E: exists, large assets (models, caches, datasets, archives) can be organized under `E:\` to reduce pressure on `D:`.

