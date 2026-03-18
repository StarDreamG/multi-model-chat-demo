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
