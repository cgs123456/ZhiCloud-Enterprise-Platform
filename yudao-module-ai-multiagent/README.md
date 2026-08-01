# yudao-module-ai-multiagent

> 基于 Spring AI 1.1.8 的多 Agent 编排模块：实现 Supervisor-Worker 架构与 ReAct（Reasoning + Acting）循环。

## 1. 模块简介

`yudao-module-ai-multiagent` 是 yudao 项目的可选模块（Spring Modulith 声明 A3），在 `yudao-module-ai` 之上提供**多 Agent 编排能力**：

- **Supervisor-Worker 编排**：Supervisor 拆解任务 → 调度 Worker 执行 → Supervisor 汇总结果
- **ReAct Agent**：Thought → Action → Observation 循环，支持多步推理与工具调用
- **拓扑配置管理**：可视化 Agent 拓扑配置（`aimultiagent_topology` 表）
- **执行日志**：完整记录每步执行过程（`aimultiagent_execution_log` 表）
- **熔断机制**：调用深度上限 + Token 预算上限双重保护

模块本身不创建 `ChatClient` Bean，运行时通过 `ChatClientHelper` 按需构建，**无 LLM API Key 时容器可正常启动**（运行时调用才会抛错）。

## 2. 核心能力

### 2.1 Supervisor-Worker 编排

| 阶段 | 实现 | 说明 |
|---|---|---|
| 任务拆解 | `SupervisorAgent.planTasks()` | 构造系统提示词，让 LLM 输出 JSON 任务列表，解析为 `AgentTask` |
| 任务分发 | `MultiAgentExecuteService` | 按 `assignedWorker` 名称从 `WorkerAgentRegistry` 获取 Worker 实例 |
| Worker 执行 | `AbstractWorkerAgent.execute()` | 子类实现具体逻辑，复用 `callLlm()` 调用 LLM |
| 结果汇总 | `SupervisorAgent.summarize()` | 构造包含所有 Worker 结果的上下文，让 LLM 生成最终答案 |

`SupervisorAgent` 输出的 JSON 任务格式：

```json
[
  {
    "taskId": "task-1",
    "description": "查询库存",
    "assignedWorker": "report-writer",
    "requiredTools": []
  }
]
```

### 2.2 ReAct Agent

基于 ReAct 论文（https://arxiv.org/abs/2210.03629）实现循环：

```
循环开始
  ↓
Thought: LLM 推理下一步
  ↓
Action: 选择工具调用 或 输出 "Final Answer"
  ↓
是 Final Answer? ──是──→ 返回最终答案
  ↓ 否
Observation: 工具执行结果反馈给 LLM
  ↓
回到循环开始
```

**熔断条件**（任一触发即终止）：

- 步数超限（默认 `maxSteps=10`）
- Token 预算超限（默认 `maxTokenBudget=4000`）
- 超时（默认 `timeoutSeconds=60`）

LLM 输出格式（严格 JSON）：

```json
{"thought": "推理内容", "action": "工具名或Final Answer", "actionInput": "工具参数JSON或最终答案"}
```

**工具来源**：

- `ToolCallbackProvider`（Spring AI 自动收集 `@Tool` 方法，如 `MethodToolCallbackProvider`）
- `List<ToolCallback>`（来自 `yudao-module-ai` 的 `AiAutoConfiguration` 注册的 MCP / 业务工具）

### 2.3 拓扑管理

`MultiAgentTopology`（DO + JSON 反序列化模型）描述一次完整编排：

- `supervisorSystemPrompt`：Supervisor 系统提示词
- `workers`：Worker 配置列表（name / description / systemPrompt / tools）
- `maxDepth`：最大调用深度（防死循环）
- `maxTokenBudget`：Token 预算上限

### 2.4 执行日志

`MultiAgentExecutionLogDO` 完整记录每次执行：

- `supervisorPlan`：Supervisor 任务拆解 JSON
- `workerResults`：Worker 执行结果 JSON
- `finalAnswer`：最终汇总答案
- `totalTokens` / `actualDepth` / `durationMs`：消耗指标
- `status`：0进行中 / 1成功 / 2失败 / 3熔断

## 3. 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| Spring AI | 1.1.8（BOM 统一管理） |
| Spring Boot | 3.5（继承自 yudao 父项目） |
| JDK | 21（继承自 yudao 父项目，支持虚拟线程） |
| ChatClient | Spring AI 1.1.8 `ChatClient.builder(chatModel).build()` |
| MyBatis Plus | 继承自 `yudao-spring-boot-starter-mybatis` |
| 多租户 | 继承自 `yudao-spring-boot-starter-biz-tenant` |

## 4. 模块结构

```
yudao-module-ai-multiagent/
├── pom.xml                                # 依赖 yudao-module-ai（optional）+ spring-ai-bom 1.1.8
└── src/main/java/cn/zhicloud/yudao/module/aimultiagent/
    ├── AiMultiAgentApplication.java       # 入口标记类（由 yudao-server 扫描加载）
    ├── package-info.java                  # Spring Modulith 模块声明（A3）
    ├── config/
    │   ├── AiMultiAgentConfiguration.java # 模块配置入口（核心 Bean 通过 @Service/@Component 自动注册）
    │   └── ChatClientHelper.java          # ChatClient 获取助手（@Autowired(required=false) AiModelService）
    ├── enums/
    │   └── ErrorCodeConstants.java        # 错误码（1-042-xxx-xxx 段）
    ├── model/
    │   ├── AgentTopology.java             # 拓扑模型（含 WorkerConfig 内部类）
    │   ├── AgentTask.java                 # 任务模型（taskId/description/assignedWorker/requiredTools/parameters）
    │   └── AgentResult.java              # 执行结果（taskId/success/output/errorMsg/tokensUsed/durationMs）
    ├── controller/admin/                  # 自动继承 /admin-api 前缀
    │   ├── react/                         # ReAct Agent 控制器
    │   │   ├── ReActAgentController.java
    │   │   └── vo/ReActRunReqVO.java
    │   ├── execute/                       # 多 Agent 编排执行控制器
    │   │   ├── MultiAgentExecuteController.java
    │   │   └── vo/（MultiAgentExecuteReqVO、MultiAgentExecuteLogRespVO）
    │   └── topology/                      # 拓扑管理控制器
    │       ├── MultiAgentTopologyController.java
    │       └── vo/（SaveReqVO、RespVO、PageReqVO）
    ├── service/
    │   ├── agent/
    │   │   ├── SupervisorAgent.java       # 任务拆解 + 结果汇总
    │   │   ├── AbstractWorkerAgent.java   # Worker 抽象基类（封装 callLlm）
    │   │   ├── DefaultReportWorker.java   # 默认 report-writer Worker（启动时自动注册）
    │   │   └── WorkerAgentRegistry.java   # Worker 注册中心（@PostConstruct 自动注册）
    │   ├── react/
    │   │   ├── ReActAgent.java            # ReAct 循环主逻辑（@Component）
    │   │   ├── ReActStep.java             # 单步记录（record：thought/action/actionInput/observation/tokenUsage）
    │   │   └── ReActResult.java           # 执行结果（record：finalAnswer/steps/totalTokenUsage/success/errorMessage）
    │   ├── topology/
    │   │   ├── MultiAgentTopologyService.java
    │   │   └── MultiAgentTopologyServiceImpl.java
    │   └── execute/
    │       ├── MultiAgentExecuteService.java
    │       └── MultiAgentExecuteServiceImpl.java
    └── dal/
        ├── mysql/
        │   ├── MultiAgentTopologyMapper.java
        │   └── MultiAgentExecutionLogMapper.java
        └── dataobject/
            ├── MultiAgentTopologyDO.java  # aimultiagent_topology
            └── MultiAgentExecutionLogDO.java  # aimultiagent_execution_log
```

## 5. API 端点

所有控制器位于 `controller.admin` 包下，自动继承 yudao 框架的 `/admin-api` 前缀。

### 5.1 ReAct Agent

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/admin-api/aimultiagent/react/run` | `aimultiagent:react:run` | 执行 ReAct Agent |

请求体 `ReActRunReqVO`：

```json
{
  "userInput": "查询并总结今天的新增订单",
  "maxSteps": 10,
  "maxTokenBudget": 4000,
  "timeoutSeconds": 60
}
```

### 5.2 多 Agent 编排执行

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/admin-api/aimultiagent/execute/run` | `aimultiagent:execute:run` | 执行多 Agent 拓扑 |
| GET | `/admin-api/aimultiagent/execute/log?id={id}` | `aimultiagent:execute:query` | 查询执行日志 |
| GET | `/admin-api/aimultiagent/execute/log/list?topologyId={id}` | `aimultiagent:execute:query` | 按拓扑 ID 查询日志列表 |

### 5.3 拓扑管理

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/admin-api/aimultiagent/topology/create` | `aimultiagent:topology:create` | 创建拓扑配置 |
| PUT | `/admin-api/aimultiagent/topology/update` | `aimultiagent:topology:update` | 更新拓扑配置 |
| DELETE | `/admin-api/aimultiagent/topology/delete?id={id}` | `aimultiagent:topology:delete` | 删除拓扑配置 |
| GET | `/admin-api/aimultiagent/topology/get?id={id}` | `aimultiagent:topology:query` | 获取拓扑详情 |
| GET | `/admin-api/aimultiagent/topology/page` | `aimultiagent:topology:query` | 拓扑分页查询 |

## 6. 设计要点

### 6.1 与 yudao-module-ai 的关系

本模块 `pom.xml` 中 `yudao-module-ai` 声明为 `<optional>true</optional>` 依赖：

- **共享 `AiModelService`**：通过 `ChatClientHelper` 调用 `AiModelService.getRequiredDefaultModel(CHAT)` 获取默认对话模型，再 `ChatClient.builder(chatModel).build()` 构建 `ChatClient`
- **共享 `ToolCallback`**：自动收集 yudao-module-ai 的 `AiAutoConfiguration` 注册的 MCP / 业务工具
- **不反向依赖**：yudao-module-ai 不依赖本模块，可独立部署

### 6.2 启动安全

为避免无 LLM API Key 时容器启动失败，所有 AI 相关 Bean 均使用可选注入：

```java
@Autowired(required = false)
private ChatClient chatClient;            // yudao-module-ai 未直接提供时为 null

@Autowired(required = false)
private ChatClientHelper chatClientHelper; // 兜底，按需构建

@Autowired(required = false)
private List<ToolCallbackProvider> toolCallbackProviders;  // Spring AI 自动注册

@Autowired(required = false)
private List<ToolCallback> toolCallbacks;  // yudao-module-ai AiAutoConfiguration 注册
```

调用时通过 `obtainChatClient()` 兜底获取，若仍不可用则返回明确错误：

```java
if (client == null) {
    return ReActResult.failure("ChatClient 不可用，请先配置 LLM API Key", new ArrayList<>(), 0);
}
```

### 6.3 熔断机制

`ReActAgent.run()` 在每次循环开始处检查：

- **超时**：`System.nanoTime() - startNanos > deadlineNanos`
- **Token 预算**：`totalTokenUsage > maxTokenBudget`
- **步数**：循环变量 `stepIndex >= maxSteps`

`MultiAgentExecuteServiceImpl` 在编排执行时检查：

- **调用深度**：`EXECUTE_DEPTH_EXCEEDED`（任务数超过 `maxDepth`）
- **Token 预算**：`EXECUTE_TOKEN_BUDGET_EXCEEDED`

### 6.4 Worker 扩展

新增自定义 Worker 只需：

1. 继承 `AbstractWorkerAgent`，实现 `getName()` / `getDescription()` / `getSupportedTools()` / `execute(AgentTask, Long)`
2. 标注 `@Component`，构造器注入 `ChatClientHelper` 和 `WorkerAgentRegistry`
3. 在 `@PostConstruct` 中调用 `registry.register(this)`

参考 `DefaultReportWorker`（`report-writer`）。

### 6.5 错误码段

使用 `1-042-xxx-xxx` 段：

- `1_042_000_000` ~ `1_042_000_002`：拓扑配置错误
- `1_042_001_000`：执行日志错误
- `1_042_002_000` ~ `1_042_002_008`：编排执行错误（含 LLM 不可用、Worker 未找到、深度/Token 熔断、Supervisor 拆解/汇总失败）

## 7. 使用前提

- **必须配置 LLM API Key**：在 yudao 后台「AI 模型管理」中配置至少一个 `CHAT` 类型模型，并设为默认（供 `AiModelService.getRequiredDefaultModel(CHAT)` 获取）
- **可选：MCP 工具**：如需 ReAct Agent 调用业务工具，需在 yudao-module-ai 中通过 `@Tool` 注解或 MCP Server 注册工具
- **数据库**：需创建 `aimultiagent_topology` 与 `aimultiagent_execution_log` 表（建表脚本请参考 sql 目录）
- **JDK 21**：本模块受益于虚拟线程，构建运行需 JDK 21+

## 8. 参考链接

- Spring AI ChatClient：https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring AI Tool Calling：https://docs.spring.io/spring-ai/reference/api/tools.html
- ReAct 论文：https://arxiv.org/abs/2210.03629
- Supervisor-Worker 模式：https://langchain-ai.github.io/langgraph/concepts/multi_agent/#supervisor
