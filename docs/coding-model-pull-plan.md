# 编码模型拉取计划（本地 Ollama）

## 本轮目标（Phase 2）

以“统一 OpenAI 兼容 API 可调用”为验收标准，优先扩展更高性能编码模型：

1. `deepseek-coder-v2-lite-instruct:7b`
2. `qwen2.5-coder:7b-instruct`

## 当前可用基线（2026-03-18）

- 已上线可用：`deepseek-coder:1.3b-instruct`（`enabled=1`）
- 已拉取可选：`gemma2:2b`
- 计划中未启用：
  - `gemma2:2b-code`
  - `deepseek-coder-v2-instruct`
  - `deepseek-coder-v2-lite-instruct`
  - `codellama-7b-code-instruct`

## 关键约束

- 本机显卡为 `GTX 1650 4GB`，不适合稳定承载 `7B` 级模型在线服务。
- 部分标签在 Ollama 侧可能出现 `manifest not found`，需先做真实标签探测。
- 若存在 E 盘，模型文件与大体积缓存可放在 E 盘目录（如 `E:\ModelStore`），D 盘优先保留源码与轻量配置。

## 部署策略

### A. 本机稳定服务（默认）

- 持续使用 `deepseek-coder:1.3b-instruct` 保障可用。
- `7B` 模型若本机可拉取，仅做功能验证，不作为默认生产路由。

### B. 高性能模型服务（目标）

- 建议将 `7B` 模型部署到高显存节点（建议 `>=16GB VRAM`）。
- 后端继续通过 `model_config` 管理模型，不改调用接口。

## 执行步骤

1. 探测 `deepseek-coder-v2-lite-instruct:7b` 与 `qwen2.5-coder:7b-instruct` 的可拉取标签。
2. 拉取成功后写入 `model_config` 并设为 `enabled=1`。
3. 验证 `/v1/models` 与 `/v1/chat/completions`。
4. 记录延迟、稳定性与资源占用，决定默认路由。

## 验收标准

- `GET /v1/models` 能看到目标模型。
- `POST /v1/chat/completions` 指定目标模型返回 200 且内容正常。
- 与当前 `1.3b` 基线相比，编码任务质量有可感知提升。

