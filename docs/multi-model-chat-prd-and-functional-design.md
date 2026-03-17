# 多模型对话 Web 应用需求与功能设计（Java + Vue）

## 1. 项目目标

构建一个前后端分离的 Web 应用：

- 前端：Vue 3（Vite）
- 后端：Java（建议 Spring Boot）
- 能在服务器部署多个大模型（本地模型、私有网关模型、云模型）
- 用户可在界面中切换模型并连续对话
- 支持基本会话管理、模型参数配置、调用监控与权限控制

## 2. 角色与使用场景

### 2.1 角色

- 普通用户：发起对话、切换模型、管理自己的会话
- 管理员：管理模型配置、查看调用统计、启停模型路由

### 2.2 核心场景

- 用户在同一会话中切换不同模型进行回答对比
- 用户创建多个会话并按主题管理
- 管理员新增一个模型接入（URL/API Key/并发限制），前台可见并可选
- 管理员查看模型调用成功率、平均耗时、错误分布

## 3. 范围定义

### 3.1 MVP（第一阶段必须）

- 账号登录（JWT）与基础权限（USER/ADMIN）
- 模型列表查询与可用状态展示
- 单轮/多轮对话
- 对话中切换模型
- SSE 流式输出
- 会话管理（新建、重命名、删除、历史消息）
- 管理端模型配置（新增、编辑、上下线）
- 基础调用日志（请求时间、耗时、token、状态码）

### 3.2 第二阶段（增强）

- 对比模式（同一问题并发调用多个模型）
- 提示词模板库
- 文档上传与 RAG 检索增强
- 敏感词审查与输出安全策略
- 配额计费与组织级权限

## 4. 功能设计

### 4.1 前端功能（Vue）

#### 页面结构

- 登录页 `/login`
- 对话页 `/chat`
- 管理页 `/admin/models`（管理员）
- 统计页 `/admin/metrics`（管理员）

#### 对话页模块

- 左侧：会话列表（按更新时间排序、搜索、创建、删除）
- 中间：消息流（用户消息、模型回复、流式状态、失败重试）
- 顶部：模型选择器（单选，展示模型标签/延迟/可用状态）
- 底部：输入框（回车发送、Shift+Enter 换行、停止生成）
- 侧边参数面板：`temperature/top_p/max_tokens` 调节

#### 交互规则

- 当前会话切换模型后，仅影响后续消息，不回改历史消息
- 发送后立即插入“占位回复”，流式逐步更新内容
- 网络错误时保留失败消息，支持“重试上一条”
- 发生 401 自动刷新登录态或跳转登录页

### 4.2 后端功能（Java）

#### 模块划分

- `auth-service`：登录、JWT 签发与校验
- `conversation-service`：会话与消息管理
- `model-gateway-service`：统一模型调用网关（适配器模式）
- `admin-service`：模型配置管理、状态开关
- `metrics-service`：调用日志与统计聚合

#### 模型适配层（关键）

统一接口 `ModelClient`：

- `chatCompletion(request)`：非流式
- `chatCompletionStream(request)`：流式
- `healthCheck()`：可用性检查

适配器实现示例：

- `OpenAICompatibleClient`
- `LocalVllmClient`
- `OllamaClient`
- `CustomHttpClient`

路由策略：

- 根据前端传入 `modelCode` 路由到对应适配器
- 不可用模型直接返回业务错误码 `MODEL_UNAVAILABLE`

### 4.3 管理功能

- 模型配置字段：`modelCode`、`displayName`、`endpoint`、`apiKey(加密)`、`timeoutMs`、`maxQps`、`enabled`
- 在线检测：管理员点击“检测连通性”实时返回结果
- 上下线开关：下线后前台不可选，历史消息不受影响

## 5. 数据设计（建议）

### 5.1 核心表

- `sys_user`：用户表
- `sys_role`：角色表
- `chat_session`：会话表（user_id, title, model_code, updated_at）
- `chat_message`：消息表（session_id, role, content, token_in, token_out, latency_ms）
- `model_config`：模型配置表
- `model_call_log`：调用日志表（request_id, model_code, status, error_code, duration_ms）

### 5.2 索引建议

- `chat_session(user_id, updated_at desc)`
- `chat_message(session_id, created_at asc)`
- `model_call_log(model_code, created_at desc)`

## 6. API 设计（MVP）

### 6.1 用户与认证

- `POST /api/auth/login`
- `GET /api/auth/me`

### 6.2 会话与消息

- `GET /api/sessions`
- `POST /api/sessions`
- `PATCH /api/sessions/{id}`
- `DELETE /api/sessions/{id}`
- `GET /api/sessions/{id}/messages`

### 6.3 对话

- `POST /api/chat/completions`（非流式）
- `POST /api/chat/completions/stream`（SSE 流式）

请求体关键字段：

- `sessionId`
- `modelCode`
- `messages[]`
- `temperature`
- `topP`
- `maxTokens`

### 6.4 管理端

- `GET /api/admin/models`
- `POST /api/admin/models`
- `PUT /api/admin/models/{id}`
- `PATCH /api/admin/models/{id}/enable`
- `POST /api/admin/models/{id}/health-check`
- `GET /api/admin/metrics/overview`

## 7. 非功能需求

- 性能：单模型并发 100（可通过网关限流）
- 可用性：核心接口可用性目标 99.9%
- 安全：
- JWT + RBAC
- API Key 加密存储（如 Jasypt/KMS）
- 输入输出日志脱敏
- 可观测性：
- 结构化日志（traceId/requestId）
- Prometheus 指标 + Grafana 看板
- 链路追踪（OpenTelemetry）

## 8. 部署架构（建议）

- 前端：Nginx 托管静态资源
- 后端：Spring Boot Docker 容器
- 数据库：MySQL 8
- 缓存：Redis（会话缓存、限流）
- 反向代理：Nginx（`/api` 转发后端，SSE 关闭缓冲）

SSE 必要配置：

- `proxy_buffering off`
- 合理设置 `proxy_read_timeout`

## 9. 里程碑计划

- M1（第 1 周）：项目骨架、登录鉴权、模型配置表、模型列表接口
- M2（第 2 周）：会话管理、流式对话、前端聊天页
- M3（第 3 周）：管理端模型管理、日志统计、基础监控
- M4（第 4 周）：联调、压测、灰度发布与验收

## 10. 验收标准（MVP）

- 用户可登录并创建会话
- 单个会话内可切换至少 3 个模型并正常回答
- 流式输出稳定，无明显卡顿
- 管理员可新增模型并上线/下线
- 可查看近 24 小时模型调用量、成功率、平均耗时
- 关键失败场景（超时、限流、鉴权失败）有明确错误提示

## 11. 技术选型建议（与你当前仓库对齐）

- 前端：保留现有 Vue 3 + Vite + Pinia 架构，新增聊天与管理路由
- 后端：将 `backend` 从 Python 占位改为 Java Spring Boot 工程
- 协议：对话接口先统一 OpenAI 兼容请求结构，便于后续接入更多模型

---

该文档用于第一轮需求冻结。下一步可直接输出：

1. 系统架构图（组件级）
2. 数据库 DDL（MySQL）
3. 后端接口 Swagger 草案
4. 前端页面原型与状态流转图
