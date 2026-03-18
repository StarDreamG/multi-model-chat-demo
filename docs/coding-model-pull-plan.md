# 编码模型拉取计划（本地 Ollama）

## 当前目标

优先部署以下编码模型（默认先入库、禁用，按资源逐步拉取并启用）：

1. gemma2:2b-code
2. deepseek-coder-v2-instruct
3. deepseek-coder-v2-lite-instruct
4. codellama-7b-code-instruct

## 本机建议优先级（GTX 1650 / 4GB）

1. gemma2:2b-code（小模型，先验证链路）
2. deepseek-coder-v2-lite-instruct（优先尝试 Lite）
3. codellama-7b-code-instruct（中等规模）
4. deepseek-coder-v2-instruct（大规模，资源充足再启用）

## 说明

- 以上模型已通过 Flyway V3 写入 `model_config` 计划列表。
- 初始状态为 `enabled=0`，避免未拉取完成时误路由。
- 实际拉取后可在管理接口中启用对应 `model_code`。

## 当前落地状态（2026-03-18）

- 已可用：`deepseek-coder:1.3b-instruct`（已拉取、已启用，作为 DeepSeek-Coder 优先可用方案）。
- 计划中（待拉取/待启用）：`gemma2:2b-code`、`deepseek-coder-v2-instruct`、`deepseek-coder-v2-lite-instruct`、`codellama-7b-code-instruct`。
- 说明：`deepseek-coder-v2*` 当前标签在本机 Ollama 拉取时返回 `manifest not found`，先以 `1.3b-instruct` 保证链路可用。
