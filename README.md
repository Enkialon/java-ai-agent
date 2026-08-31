# java-ai-agent

一个用 **Java + Quarkus** 从零搭建的 AI 编码助手学习项目。目标是理解「Agent 循环、工具调用、权限审批、SSE 流式输出、工作区沙箱」等常见 Agent 系统的核心机制，而不是复刻某个商业产品。

> 本项目仅供学习交流，API Key 与工具执行均在本地/你控制的环境中进行，请自行评估安全风险。

---

## 功能概览

- **Agent Loop**：模型回合 → 工具调用 → 再回合，最多 20 轮
- **内置工具**：`read` / `write` / `edit` 文件，`bash`（POSIX）或 `powershell`（Windows）执行命令
- **多模型后端**：`stub`（本地占位）、DeepSeek、OpenAI 兼容 API
- **工具权限**：`allow` / `deny` / `ask`（需前端或 API 审批）
- **Web UI**：内置聊天界面，支持选择本机工作目录、流式展示、Diff 预览
- **SSE 事件流**：通过 HTTP Server-Sent Events 推送 Agent 运行过程
- **工作区绑定**：每个会话绑定一个本机目录，工具只能在该目录内操作
- **Hook 扩展点**：生命周期、模型、工具等 Hook，便于插入自定义逻辑

---

## 技术栈

| 组件 | 说明 |
|------|------|
| Java 25 | 语言与运行时（使用虚拟线程跑 Agent 核心） |
| Quarkus 3.39 | REST、CDI、打包 |
| Gradle 9 | 构建与分发 |
| OpenAI Java SDK | 调用 DeepSeek / OpenAI 兼容接口 |
| Jackson YAML | 加载 `agent.yaml` 配置 |

---

## 环境要求

- **JDK 25**（开发与打包均需；可用 [SDKMAN](https://sdkman.io/) 或 [Eclipse Temurin](https://adoptium.net/) 安装）
- 可选：**DeepSeek** 或 **OpenAI** API Key（不配置则默认使用 `stub` 模型，仅用于跑通流程）

验证 Java 版本：

```bash
java -version   # 应显示 25.x
```

---

## 快速开始（开发模式）

```bash
# 克隆仓库
git clone git@github.com:Enkialon/java-ai-agent.git
cd java-ai-agent

# 可选：配置 API Key（使用 deepseek 时需要）
export DEEPSEEK_API_KEY=sk-your-key

# 可选：复制并编辑本地配置（不复制则使用 classpath 内默认 agent.yaml）
cp agent.example.yaml agent.yaml

# 启动开发服务器（热重载）
./gradlew quarkusDev
```

浏览器打开：**http://localhost:8080**

在 Web UI 中先「选择目录」绑定工作区，再发送消息即可开始对话。

---

## 配置说明

Agent 行为由 **`agent.yaml`** 控制。查找顺序（优先级从高到低）：

1. JVM 系统属性 `-Dagent.config=/path/to/agent.yaml`
2. 环境变量 `AGENT_CONFIG`
3. 当前工作目录下的 `./agent.yaml`（适合 jar 旁挂配置）
4. classpath 内 `/agent.yaml`（开发默认）

字符串支持环境变量占位：`${ENV_NAME}` 或 `${ENV_NAME:default}`。

### 示例

复制仓库根目录的 `agent.example.yaml` 为 `agent.yaml` 并按需修改：

```yaml
agent:
  model:
    active: deepseek          # 当前使用的客户端实例名
    clients:
      stub:
        type: stub              # 本地占位，不调用外部 API

      deepseek:
        type: deepseek
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com
        model: deepseek-v4-flash

      openai:
        type: openai
        api-key: ${OPENAI_API_KEY:}
        base-url: https://api.openai.com/v1
        model: gpt-4o

  permissions:
    bash: ask      # allow | deny | ask
    write: ask     # 控制 write / edit 工具
    network: deny  # 预留

  loop:
    max-turns: 20  # Agent 最大模型回合数
```

| 配置项 | 说明 |
|--------|------|
| `model.active` | 选用 `clients` 下的哪个实例 |
| `model.clients.*.type` | `stub` / `deepseek` / `openai` |
| `permissions.bash` | Shell 工具（bash 或 powershell）权限 |
| `permissions.write` | 写文件 / 编辑文件工具权限 |
| `permissions.ask` | 需审批时，SSE 推送 `tool_approval_required` 事件 |
| `loop.max-turns` | Agent 最大模型回合数（默认 20） |

> **安全提示**：不要把真实 API Key 提交进 Git。`.gitignore` 已忽略根目录 `agent.yaml`。

### HTTP 服务配置

`src/main/resources/application.properties`：

```properties
quarkus.http.host=0.0.0.0   # 允许局域网访问
quarkus.http.port=8080
```

---

## 打包与分发

项目提供四种分发方式，**每次只运行其中一个 Gradle 任务**（不要同时跑多个，会冲突）：

| 任务 | 产物目录 | 说明 |
|------|----------|------|
| `./gradlew distFast` | `build/dist/fast/` | Quarkus 默认 fast-jar，需本机 Java 25+ |
| `./gradlew distUber` | `build/dist/uber/` | 单文件 uber-jar，需本机 Java 25+ |
| `./gradlew distSlim` | `build/dist/slim/` | Tree-shaking 精简 uber-jar，需本机 Java 25+ |
| `./gradlew distJlink` | `build/dist/jlink/` | uber-jar + jlink 精简 JRE，**目标机无需安装 Java** |

每个产物目录均包含：

- `run.sh` / `run.bat` — 启动脚本
- `agent.example.yaml` — 部署配置示例

### distUber / distSlim / distFast

目标机器需已安装 **Java 25+**：

```bash
./gradlew distUber
cd build/dist/uber
cp agent.example.yaml agent.yaml
export DEEPSEEK_API_KEY=sk-your-key
./run.sh
```

### distJlink（自带运行时）

构建时会用 `jdeps` + `jlink` 生成精简 Java 25 Runtime。**产物须在与构建相同的 OS/CPU 架构上运行**。

```bash
./gradlew distJlink
cd build/dist/jlink

cp agent.example.yaml agent.yaml
export DEEPSEEK_API_KEY=sk-your-key
./run.sh

# 等价于：
# ./runtime/bin/java -jar app/java-ai-agent-runner.jar
```

### 运行时参数

```bash
# 指定配置文件
AGENT_CONFIG=/path/to/agent.yaml ./run.sh
./run.sh -Dagent.config=/path/to/agent.yaml

# JVM 参数
JAVA_OPTS='-Xmx512m' ./run.sh
```

---

## HTTP API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/agent/chat` | 发送消息，响应为 SSE（`Content-Type: text/event-stream`） |
| `GET` | `/api/agent/status` | 当前会话 ID、工作区、模型信息 |
| `PUT` | `/api/agent/session/workspace` | 绑定工作目录 `{"path":"/your/project"}` |
| `GET` | `/api/agent/session/workspace` | 查询当前工作区 |
| `GET` | `/api/agent/filesystem` | 浏览本机目录（供 UI 选目录） |
| `POST` | `/api/agent/session/approvals/{callId}/approve` | 批准工具调用 |
| `POST` | `/api/agent/session/approvals/{callId}/deny` | 拒绝工具调用 |

### SSE 事件类型

| type | 含义 |
|------|------|
| `agent_start` | 一次 Run 开始 |
| `text_delta` | 模型文本增量 |
| `tool_approval_required` | 需人工审批 |
| `tool_approval_resolved` | 审批结果 |
| `tool_start` / `tool_end` | 工具执行起止 |
| `message_end` | 单条消息结束 |
| `agent_end` | Run 结束 |

---

## 项目结构

```
src/main/java/org/example/agent/
├── api/              # REST 适配层（HTTP / SSE）
├── application/      # 应用服务：Agent 编排、Loop、权限、工作区
├── domain/           # 领域模型：Session、Tool、Workspace、Permission
└── infrastructure/   # 基础设施：LLM 客户端、配置、沙箱、内置工具

src/main/resources/
├── agent.yaml        # 默认配置（classpath）
├── application.properties
└── META-INF/resources/   # Web UI 静态资源（index.html / app.js / style.css）
```

核心流程：

```
用户消息 → AgentService → AgentLoopService
                              ↓
                    ModelTurnService（调用 LLM）
                              ↓
                    ToolExecutionService（执行工具，含权限检查）
                              ↓
                    AgentEventSink → SSE 推送到前端
```

---

## 开发与测试

```bash
# 运行全部单元测试
./gradlew test

# 查看测试报告
open build/reports/tests/test/index.html   # macOS
xdg-open build/reports/tests/test/index.html   # Linux

# 仅编译，不启动
./gradlew compileJava
```

常用 Gradle 任务：

```bash
./gradlew quarkusDev      # 开发模式（端口 8080）
./gradlew quarkusBuild    # 生产构建
./gradlew tasks           # 查看所有任务
```

---

## 学习路线建议

如果你想通过本项目系统学习 Agent 工程，可以按以下顺序阅读代码：

1. **`AgentChatResource`** — HTTP/SSE 入口，虚拟线程与 Mutiny 边界
2. **`AgentLoopService`** — 最简 Agent 状态机
3. **`ModelTurnService` + `OpenAiCompatibleModelClient`** — 如何调用 LLM 并解析 tool call
4. **`ToolExecutionService` + `ToolPermissionService`** — 工具执行与权限
5. **`BashTool` / `PosixSandboxRuntime`** — 命令沙箱与工作目录约束
6. **`AgentEvent` + `app.js`** — 前后端事件协议

---

## 已知限制

- `network` 权限项为预留，尚未实现网络工具
- Web UI 依赖 CDN 加载 marked / diff2html（离线环境需自行替换）
- `distJlink` 产物与构建平台绑定，不能跨 OS/架构分发

---

## 贡献与反馈

欢迎 Issue 和 Pull Request。本项目定位为**学习参考**，API 与架构可能随学习进度调整，不保证向后兼容。

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
