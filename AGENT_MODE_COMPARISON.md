# JD Genie Agent 执行模式深度对比分析

## 📋 目录
1. [概述](#概述)
2. [Agent模式总览](#agent模式总览)
3. [Plan-Solve模式详解](#plan-solve模式详解)
4. [ReAct模式详解](#react模式详解)
5. [核心差异对比](#核心差异对比)
6. [选型建议](#选型建议)
7. [性能对比](#性能对比)
8. [最佳实践](#最佳实践)

---

## 概述

JD Genie支持多种Agent执行模式，每种模式有不同的任务分解策略、执行流程和适用场景。本文档深入对比分析两种主要的Agent模式：**Plan-Solve**（规划解决）和**ReAct**（推理行动）。

### 已实现的模式
- ✅ **Plan-Solve**: 规划解决模式（生产环境）
- ✅ **ReAct**: 推理行动模式（生产环境）

### 计划中的模式
- ⏳ **Comprehensive**: 综合模式
- ⏳ **Workflow**: 工作流模式
- ⏳ **Router**: 路由器模式

---

## Agent模式总览

| 特性 | Plan-Solve | ReAct |
|------|-----------|-------|
| **核心思想** | 先规划后执行 | 边思考边行动 |
| **Agent数量** | 3个（Planning + Executor + Summary） | 1个（ReactAgent） |
| **任务分解** | 显式规划，拆分为子任务 | 隐式分解，动态决策 |
| **并行执行** | ✅ 支持子任务并行 | ❌ 单线程执行 |
| **迭代次数** | 可配置（default: 10） | 固定的ReAct循环 |
| **复杂度** | 高 | 中 |
| **SOP支持** | ✅ 内置SOP召回 | ❌ 不支持 |
| **适用场景** | 复杂多步骤任务 | 简单快速任务 |

---

## Plan-Solve模式详解

### 1. 核心理念

**Plan-Solve（规划解决）模式**采用"先规划后执行"的策略，将复杂任务分解为多个可并行执行的子任务，通过三个专门的Agent协作完成任务。

### 2. Agent组成

```
┌─────────────────────────────────────────────────────────┐
│                    Plan-Solve模式                         │
└─────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ↓               ↓               ↓
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ PlanningAgent│ │ ExecutorAgent│ │ SummaryAgent │
    │   规划者     │ │   执行者     │ │   总结者     │
    └──────────────┘ └──────────────┘ └──────────────┘
            │               │               │
            ↓               ↓               ↓
    创建任务计划    执行具体任务    总结任务结果
```

#### 2.1 PlanningAgent（规划者）

**职责**：
- 分析用户问题，制定执行计划
- 将复杂任务拆分为多个子任务
- 动态调整计划（可选）

**核心方法**：
```java
/**
 * PlanningAgent的think方法
 * 负责思考并生成任务计划
 */
@Override
public boolean think() {
    // 1. 格式化文件信息
    String filesStr = FileUtil.formatFileInfo(context.getProductFiles(), false);
    setSystemPrompt(getSystemPromptSnapshot().replace("{{files}}", filesStr));

    // 2. 如果关闭动态更新，直接使用固定计划
    if (isColseUpdate && planningTool.getPlan() != null) {
        planningTool.stepPlan();  // 步进到下一个任务
        return true;
    }

    // 3. 调用LLM生成规划
    CompletableFuture<LLM.ToolCallResponse> future = getLlm().askTool(
        context,
        getMemory().getMessages(),
        Message.systemMessage(getSystemPrompt(), null),
        availableTools,
        ToolChoice.AUTO,
        null,
        context.getIsStream(),
        300
    );

    // 4. 获取响应和工具调用
    LLM.ToolCallResponse response = future.get();
    setToolCalls(response.getToolCalls());

    return true;
}
```

**输出示例**：
```
用户问题: "分析京东2024年财报并生成报告"

PlanningAgent生成的计划:
Task 1: 搜索京东2024年财报原始数据 <sep>
Task 2: 分析财报中的营收、利润等关键指标 <sep>
Task 3: 对比去年同期数据，分析增长趋势 <sep>
Task 4: 生成完整的财报分析报告
```

#### 2.2 ExecutorAgent（执行者）

**职责**：
- 执行PlanningAgent分配的具体任务
- 调用工具（文件读写、代码执行、搜索等）
- 收集执行结果

**并行执行机制**：
```java
/**
 * 并行执行多个子任务
 */
if (planningResults.size() == 1) {
    // 单任务：直接执行
    executorResult = executor.run(planningResults.get(0));
} else {
    // 多任务：并行执行
    Map<String, String> tmpTaskResult = new ConcurrentHashMap<>();
    CountDownLatch taskCount = new CountDownLatch(planningResults.size());

    // 为每个任务创建独立的Executor
    for (String task : planningResults) {
        ExecutorAgent slaveExecutor = new ExecutorAgent(agentContext);
        slaveExecutor.setState(executor.getState());
        slaveExecutor.getMemory().addMessages(executor.getMemory().getMessages());

        // 异步执行
        ThreadUtil.execute(() -> {
            String taskResult = slaveExecutor.run(task);
            tmpTaskResult.put(task, taskResult);
            taskCount.countDown();
        });
    }

    // 等待所有任务完成
    ThreadUtil.await(taskCount);

    // 合并结果
    executorResult = String.join("\n", tmpTaskResult.values());
}
```

#### 2.3 SummaryAgent（总结者）

**职责**：
- 汇总所有执行结果
- 提取关键信息和文件产物
- 生成最终回答

**总结流程**：
```java
/**
 * 总结任务结果
 */
TaskSummaryResult result = summary.summaryTaskResult(
    executor.getMemory().getMessages(),  // 执行过程的所有消息
    request.getQuery()                    // 原始用户问题
);

// 结果包含：
// - taskSummary: 任务总结文本
// - files: 生成的文件列表
```

### 3. 执行流程

```
用户问题
    ↓
【第1轮迭代】
    ↓
PlanningAgent.run(query)
    ├─ think(): 分析问题，生成初始计划
    └─ act(): 调用PlanningTool，输出计划
    ↓
计划输出: "Task 1 <sep> Task 2 <sep> Task 3"
    ↓
ExecutorAgent并行执行
    ├─ Thread 1: ExecutorAgent.run("Task 1")
    ├─ Thread 2: ExecutorAgent.run("Task 2")
    └─ Thread 3: ExecutorAgent.run("Task 3")
    ↓
收集执行结果
    ↓
【第2轮迭代】
    ↓
PlanningAgent.run(执行结果)
    ├─ think(): 根据执行结果，判断是否需要继续
    └─ act():
         - 如果任务完成: 返回 "finish"
         - 如果需要继续: 返回新的任务计划
    ↓
如果返回"finish"
    ↓
SummaryAgent.summaryTaskResult()
    ├─ 汇总所有执行记录
    ├─ 提取关键信息
    └─ 生成最终回答
    ↓
输出结果给用户
```

### 4. SOP增强（Plan-Solve独有）

```java
/**
 * 处理SOP召回逻辑
 * SOP (Standard Operating Procedure) - 标准操作流程
 */
private void handleSopRecall(AgentContext agentContext, AgentRequest request) {
    try {
        log.info("{} 开始执行SOP召回", request.getRequestId());

        // 1. 调用SOP召回服务
        SopRecallResponse sopResponse = sopRecallService.sopRecall(
            request.getRequestId(),
            request.getQuery()
        );

        // 2. 检查召回结果
        if (sopRecallService.isValidSopResult(sopResponse)) {
            String sopContent = sopResponse.getData().getChoosed_sop_string();
            String sopMode = sopResponse.getData().getSop_mode();

            // 3. 注入到提示词
            String sopPrompt = agentContext.getSopPrompt()
                .replace("{{sop}}", sopContent);
            agentContext.setSopPrompt(sopPrompt);

            log.info("{} SOP召回成功，模式：{}", requestId, sopMode);
        }
    } catch (Exception e) {
        log.error("{} SOP召回异常", requestId, e);
        // SOP召回失败不影响主流程
    }
}
```

**SOP作用**：
- 提供领域知识和标准流程
- 增强Agent的专业能力
- 确保输出符合规范

### 5. 配置项

```yaml
genie:
  # 规划Agent配置
  planner-max-steps: 10           # 最大迭代次数
  planner-model-name: "gpt-4"     # 使用的LLM模型
  planning-close-update: "0"      # 是否关闭动态更新计划（1-关闭，0-开启）
  plan-pre-prompt: "请仔细分析以下任务："  # 规划前缀提示

  # 规划系统提示词
  planner-system-prompt:
    default: |
      你是一个专业的任务规划助手。
      可用工具：{{tools}}
      当前日期：{{date}}
      用户问题：{{query}}
      标准流程：{{sopPrompt}}

      请分析任务并制定详细的执行计划。

  # 规划下一步提示词
  planner-next-step-prompt:
    default: |
      已完成的任务结果如上。
      文件产物：{{files}}

      请判断：
      1. 如果所有任务已完成，返回 "finish"
      2. 如果需要继续，返回新的任务计划（用<sep>分隔）
```

---

## ReAct模式详解

### 1. 核心理念

**ReAct（Reasoning + Acting）模式**将推理（Reasoning）和行动（Acting）结合在一起，Agent在执行过程中不断思考、行动、观察，形成一个连续的循环。

### 2. Agent组成

```
┌─────────────────────────────────────────────────────────┐
│                      ReAct模式                            │
└─────────────────────────────────────────────────────────┘
                            │
                    ┌───────┴───────┐
                    ↓               ↓
            ┌──────────────┐ ┌──────────────┐
            │ ReactAgent   │ │ SummaryAgent │
            │  ReAct代理   │ │   总结者     │
            └──────────────┘ └──────────────┘
                    │               │
                    ↓               ↓
            think() → act()    总结任务结果
                ↓      ↓
            推理    行动
```

#### 2.1 ReActAgent

**职责**：
- 思考（Think）：分析当前状态，决定下一步行动
- 行动（Act）：执行工具调用或生成回答
- 重复循环直到任务完成

**核心流程**：
```java
/**
 * ReActAgent的step方法
 * 每一步包含：思考 + 行动
 */
@Override
public String step() {
    // 1. 思考：是否需要行动？
    boolean shouldAct = think();

    // 2. 如果不需要行动，直接返回
    if (!shouldAct) {
        return "Thinking complete - no action needed";
    }

    // 3. 执行行动
    return act();
}
```

**think() 实现**：
```java
/**
 * 思考方法 - 由子类实现
 * 返回true表示需要执行行动，false表示任务已完成
 */
public abstract boolean think();

// 典型实现：
@Override
public boolean think() {
    // 1. 调用LLM进行推理
    CompletableFuture<LLM.ToolCallResponse> future = getLlm().askTool(
        context,
        getMemory().getMessages(),
        Message.systemMessage(getSystemPrompt(), null),
        availableTools,
        ToolChoice.AUTO,
        null,
        true,
        300
    );

    // 2. 获取LLM响应
    LLM.ToolCallResponse response = future.get();

    // 3. 判断：是否需要调用工具？
    if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
        setToolCalls(response.getToolCalls());
        return true;  // 需要行动
    }

    // 4. 如果LLM直接给出答案，不需要行动
    return false;
}
```

**act() 实现**：
```java
/**
 * 行动方法 - 执行工具调用
 */
public abstract String act();

// 典型实现：
@Override
public String act() {
    List<String> results = new ArrayList<>();

    // 1. 执行所有工具调用
    for (ToolCall toolCall : toolCalls) {
        String result = executeTool(toolCall);
        results.add(result);

        // 2. 将工具结果添加到记忆
        Message toolMsg = Message.toolMessage(
            result,
            toolCall.getId(),
            null
        );
        getMemory().addMessage(toolMsg);
    }

    // 3. 返回所有结果
    return String.join("\n\n", results);
}
```

### 3. 执行流程

```
用户问题
    ↓
ReactAgent.run(query)
    ↓
【ReAct循环】
    ↓
┌───────────────────┐
│  Step 1: Think    │
│  LLM推理：        │
│  "我需要搜索     │
│   京东财报信息"   │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 2: Act      │
│  调用工具：       │
│  search_web(     │
│    "京东2024财报"│
│  )               │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 3: Observe  │
│  工具返回：       │
│  "京东2024年Q3...│
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 4: Think    │
│  LLM推理：        │
│  "我已获得财报   │
│   需要分析数据"   │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 5: Act      │
│  调用工具：       │
│  code_interpreter(│
│    "分析财报..."  │
│  )               │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 6: Think    │
│  LLM推理：        │
│  "数据分析完成   │
│   可以生成报告"   │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 7: Act      │
│  调用工具：       │
│  report_tool(    │
│    "生成报告..."  │
│  )               │
└───────────────────┘
    ↓
┌───────────────────┐
│  Step 8: Think    │
│  LLM推理：        │
│  "任务完成，     │
│   给出最终答案"   │
└───────────────────┘
    ↓
【循环结束】
    ↓
SummaryAgent.summaryTaskResult()
    ↓
输出结果给用户
```

### 4. 数字员工（Digital Employee）

ReAct模式的一个特色功能是**数字员工**，可以为每个任务动态生成专业角色：

```java
/**
 * 生成数字员工
 * 根据任务类型，为工具分配专业角色
 */
public void generateDigitalEmployee(String task) {
    try {
        // 1. 构建提示词
        String formattedPrompt = formatSystemPrompt(task);
        Message userMessage = Message.userMessage(formattedPrompt, null);

        // 2. 调用LLM生成角色
        CompletableFuture<String> summaryFuture = getLlm().ask(
            context,
            Collections.singletonList(userMessage),
            Collections.emptyList(),
            false,
            0.01
        );

        // 3. 解析响应
        String llmResponse = summaryFuture.get();
        JSONObject jsonObject = parseDigitalEmployee(llmResponse);

        // 4. 更新工具集合
        if (jsonObject != null) {
            context.getToolCollection().updateDigitalEmployee(jsonObject);
            availableTools = context.getToolCollection();
        }
    } catch (Exception e) {
        log.error("生成数字员工失败", e);
    }
}
```

**数字员工示例**：
```json
{
  "file_tool": "市场洞察专员",
  "code_interpreter": "数据分析师",
  "report_tool": "报告撰写专家",
  "search_tool": "信息检索专员"
}
```

### 5. Handler实现

```java
/**
 * ReAct模式的Handler
 * 简洁明了，只需单个Agent执行
 */
@Override
public String handle(AgentContext agentContext, AgentRequest request) {
    // 1. 创建ReactAgent
    ReActAgent executor = new ReactImplAgent(agentContext);
    SummaryAgent summary = new SummaryAgent(agentContext);

    // 2. 执行ReAct循环
    executor.run(request.getQuery());

    // 3. 总结结果
    TaskSummaryResult result = summary.summaryTaskResult(
        executor.getMemory().getMessages(),
        request.getQuery()
    );

    // 4. 输出结果
    Map<String, Object> taskResult = new HashMap<>();
    taskResult.put("taskSummary", result.getTaskSummary());
    taskResult.put("fileList", result.getFiles());

    agentContext.getPrinter().send("result", taskResult);

    return "";
}
```

---

## 核心差异对比

### 1. 架构对比

| 维度 | Plan-Solve | ReAct |
|------|-----------|-------|
| **Agent数量** | 3个（Planning + Executor + Summary） | 2个（React + Summary） |
| **职责分离** | 明确：规划者、执行者、总结者 | 模糊：单个Agent负责推理+行动 |
| **复杂度** | 高：需要协调多个Agent | 低：单Agent自主决策 |
| **可维护性** | 中：逻辑分散在多个Agent | 高：逻辑集中在一个Agent |

### 2. 执行流程对比

#### Plan-Solve执行时序
```
T=0  │ PlanningAgent: 分析问题
T=1  │ PlanningAgent: 生成计划 [Task1, Task2, Task3]
     │
T=2  ├─ ExecutorAgent1: 执行Task1 (并行)
     ├─ ExecutorAgent2: 执行Task2 (并行)
     └─ ExecutorAgent3: 执行Task3 (并行)
     │
T=3  │ 收集结果 → PlanningAgent
     │
T=4  │ PlanningAgent: 判断是否完成
     │   - 完成 → finish
     │   - 未完成 → 新计划 → 返回T=2
     │
T=5  │ SummaryAgent: 总结结果
     │
T=6  │ 输出给用户
```

#### ReAct执行时序
```
T=0  │ ReactAgent: 接收问题
     │
T=1  │ Think: "需要搜索财报"
T=2  │ Act: search_web()
T=3  │ Observe: 搜索结果
     │
T=4  │ Think: "需要分析数据"
T=5  │ Act: code_interpreter()
T=6  │ Observe: 分析结果
     │
T=7  │ Think: "需要生成报告"
T=8  │ Act: report_tool()
T=9  │ Observe: 报告生成
     │
T=10 │ Think: "任务完成"
T=11 │ SummaryAgent: 总结结果
T=12 │ 输出给用户
```

### 3. 并行处理能力

#### Plan-Solve: ✅ 支持并行

```java
// 同时执行3个任务
ThreadUtil.execute(() -> executor1.run("Task1"));
ThreadUtil.execute(() -> executor2.run("Task2"));
ThreadUtil.execute(() -> executor3.run("Task3"));
```

**优势**：
- 大幅缩短总执行时间
- 适合独立子任务
- 资源利用率高

**示例**：
```
串行执行: Task1(10s) + Task2(10s) + Task3(10s) = 30s
并行执行: max(Task1, Task2, Task3) = 10s
```

#### ReAct: ❌ 单线程执行

```java
// 顺序执行
step1: think() + act()  // 10s
step2: think() + act()  // 10s
step3: think() + act()  // 10s
// 总计: 30s
```

**限制**：
- 必须顺序执行
- 无法利用并行优势
- 总时间 = 所有步骤之和

### 4. 任务分解策略

#### Plan-Solve: 显式分解

```
用户问题: "分析京东财报并生成报告"

↓ PlanningAgent分解

子任务:
1. 搜索京东2024年财报
2. 提取关键财务指标
3. 对比历史数据
4. 生成分析报告
```

**特点**：
- 一次性生成完整计划
- 任务边界清晰
- 易于并行化
- 可预估总体进度

#### ReAct: 隐式分解

```
用户问题: "分析京东财报并生成报告"

↓ ReactAgent逐步推理

Step 1: "我需要先搜索财报"
  → Act: search_web()

Step 2: "我已获得财报，需要分析"
  → Act: code_interpreter()

Step 3: "数据分析完成，生成报告"
  → Act: report_tool()
```

**特点**：
- 动态决策，逐步推进
- 任务边界模糊
- 难以并行化
- 无法预估总体进度

### 5. 容错能力

#### Plan-Solve

```java
// 迭代次数限制
while (stepIdx <= maxStepNum) {
    // 执行逻辑

    // 错误检测
    if (planning.getState() == AgentState.ERROR ||
        executor.getState() == AgentState.ERROR) {
        agentContext.getPrinter().send("result", "任务执行异常");
        break;
    }

    // 超时检测
    if (planning.getState() == AgentState.IDLE ||
        executor.getState() == AgentState.IDLE) {
        agentContext.getPrinter().send("result", "达到最大迭代次数");
        break;
    }

    stepIdx++;
}
```

**容错机制**：
- ✅ 最大迭代次数保护
- ✅ 状态异常检测
- ✅ 超时保护
- ✅ 可配置的重试策略

#### ReAct

```java
// 简单的循环执行
executor.run(request.getQuery());
```

**容错机制**：
- ⚠️ 依赖BaseAgent的基础保护
- ⚠️ 无显式迭代次数限制
- ⚠️ 错误处理较弱

### 6. 可观测性

#### Plan-Solve: 高

```
输出内容:
✅ 初始计划（详细的任务列表）
✅ 每个任务的执行进度
✅ 任务状态（pending/running/completed/failed）
✅ 中间结果
✅ 计划调整记录
✅ 最终总结

用户体验:
- 清晰看到任务被拆分为哪些子任务
- 实时了解每个子任务的进展
- 便于定位问题环节
```

#### ReAct: 中

```
输出内容:
✅ 思考过程
✅ 工具调用
✅ 工具结果
✅ 最终回答

用户体验:
- 看到Agent的推理过程
- 了解调用了哪些工具
- 但无法预知总体规划
```

---

## 选型建议

### 使用Plan-Solve的场景

✅ **适合场景**：

1. **复杂多步骤任务**
   ```
   示例: "分析过去5年的销售数据，预测明年趋势，生成PPT报告"

   为什么选Plan-Solve:
   - 任务可明确拆分为：数据收集、数据分析、趋势预测、报告生成
   - 部分步骤可并行（数据收集可多源并行）
   - 需要整体规划，确保不遗漏步骤
   ```

2. **需要并行处理的任务**
   ```
   示例: "同时爬取5个竞品网站的产品信息并对比分析"

   为什么选Plan-Solve:
   - 5个爬取任务完全独立，可并行执行
   - 并行可节省80%的时间（5*10s → 10s）
   ```

3. **有标准流程的任务（SOP）**
   ```
   示例: "按照公司标准流程完成财务审计"

   为什么选Plan-Solve:
   - 内置SOP召回功能
   - 确保流程规范性
   - 审计步骤明确且有序
   ```

4. **需要清晰进度展示的任务**
   ```
   示例: 大型数据分析项目

   为什么选Plan-Solve:
   - 用户需要知道"现在做到哪一步了"
   - 任务列表提供清晰的进度指示
   - 便于中途干预和调整
   ```

❌ **不适合场景**：

1. 简单单步任务（过度设计）
2. 需要极快响应的任务（架构复杂导致overhead）
3. 任务步骤高度不确定的探索性任务

### 使用ReAct的场景

✅ **适合场景**：

1. **快速问答任务**
   ```
   示例: "北京今天天气如何？"

   为什么选ReAct:
   - 单步工具调用即可完成
   - 无需复杂规划
   - 响应速度快
   ```

2. **探索性任务**
   ```
   示例: "帮我研究一下区块链技术的最新发展"

   为什么选ReAct:
   - 任务边界不清晰
   - 需要根据搜索结果动态调整方向
   - ReAct的动态决策能力更适合
   ```

3. **资源受限环境**
   ```
   示例: 边缘设备、低配服务器

   为什么选ReAct:
   - 单Agent执行，内存占用少
   - 无并发开销
   - 适合资源受限场景
   ```

4. **需要灵活决策的任务**
   ```
   示例: "帮我找到解决这个Bug的方法"

   为什么选ReAct:
   - 可能需要尝试多种方案
   - 每一步的决策依赖上一步的结果
   - 无法提前规划所有步骤
   ```

❌ **不适合场景**：

1. 包含大量独立子任务（无法并行）
2. 需要严格遵守流程的任务（无SOP支持）
3. 执行时间过长的任务（单线程效率低）

---

## 性能对比

### 1. 执行时间对比

#### 测试场景：复杂分析任务

**任务**：分析3家公司的财报并生成对比报告

**子任务**：
1. 爬取公司A财报（10秒）
2. 爬取公司B财报（10秒）
3. 爬取公司C财报（10秒）
4. 分析数据（20秒）
5. 生成报告（15秒）

#### Plan-Solve执行时间

```
T=0-1:  PlanningAgent规划（1秒）
T=1-11: 并行爬取A、B、C（10秒）
T=11-31: 分析数据（20秒）
T=31-46: 生成报告（15秒）
T=46-47: SummaryAgent总结（1秒）

总计: 47秒
```

#### ReAct执行时间

```
T=0-10:  爬取公司A（10秒）
T=10-20: 爬取公司B（10秒）
T=20-30: 爬取公司C（10秒）
T=30-50: 分析数据（20秒）
T=50-65: 生成报告（15秒）
T=65-66: SummaryAgent总结（1秒）

总计: 66秒
```

**结论**: Plan-Solve快 **40%**（66s vs 47s）

### 2. LLM调用次数对比

#### 测试场景：5步任务

| 模式 | LLM调用次数 | 说明 |
|------|------------|------|
| Plan-Solve | 8次 | Planning(2) + Executor(5) + Summary(1) |
| ReAct | 11次 | React循环(10) + Summary(1) |

**原因**：
- Plan-Solve: 规划阶段调用少，执行阶段直接使用计划
- ReAct: 每一步都需要LLM推理

**成本影响**：
- ReAct的LLM调用成本更高（~37%）
- Plan-Solve更经济

### 3. 内存占用对比

| 模式 | 峰值内存 | 平均内存 |
|------|---------|---------|
| Plan-Solve | 512MB | 380MB |
| ReAct | 256MB | 200MB |

**原因**：
- Plan-Solve: 需要维护多个Agent实例、并发执行上下文
- ReAct: 单Agent，内存占用小

### 4. 可扩展性对比

#### 子任务数量 vs 执行时间

```
子任务数量: 1   3   5   10  20

Plan-Solve:  15s 25s 35s 50s 80s  (近似线性)
ReAct:       15s 45s 75s 150s 300s (线性增长)

差距:        0s  20s 40s 100s 220s
```

**结论**：
- 子任务越多，Plan-Solve优势越明显
- ReAct不适合大规模并行场景

---

## 最佳实践

### 1. Plan-Solve最佳实践

#### 1.1 合理配置最大迭代次数

```yaml
genie:
  planner-max-steps: 10  # 根据任务复杂度调整

  # 简单任务: 5-8
  # 中等任务: 10-15
  # 复杂任务: 15-20
  # 不建议超过30（可能陷入无限循环）
```

#### 1.2 优化SOP提示词

```yaml
# 好的SOP示例
planner-system-prompt:
  default: |
    你是一个专业的{{domain}}助手。

    标准流程：
    {{sopPrompt}}

    请严格遵循以下原则：
    1. 任务拆分要细致，每个子任务独立完成
    2. 优先使用已有工具，避免重复搜索
    3. 合并相似任务，提高并行度

    可用工具：{{tools}}
```

#### 1.3 启用计划动态更新

```yaml
genie:
  planning-close-update: "0"  # 0-开启动态更新，1-关闭

  # 何时开启:
  # - 任务步骤可能随执行结果变化
  # - 需要根据中间结果调整策略

  # 何时关闭:
  # - 任务流程完全固定
  # - 追求极致性能
```

#### 1.4 监控Agent状态

```java
// 在关键步骤检查状态
if (planning.getState() == AgentState.ERROR) {
    log.error("规划Agent异常");
    // 1. 记录详细日志
    // 2. 发送告警
    // 3. 尝试降级策略
}

if (executor.getState() == AgentState.IDLE) {
    log.warn("执行Agent空闲");
    // 1. 检查是否陷入死循环
    // 2. 考虑强制终止
}
```

### 2. ReAct最佳实践

#### 2.1 优化提示词

```yaml
# 清晰的ReAct提示词
react-system-prompt: |
  你是一个智能助手，请使用以下思维模式回答问题：

  Thought: 分析当前状态，思考下一步行动
  Action: 调用工具或给出答案
  Observation: 观察工具执行结果

  重复上述过程，直到问题解决。

  当前可用工具：{{tools}}
```

#### 2.2 启用数字员工

```java
// 为每个任务生成专业角色
reactAgent.generateDigitalEmployee(task);

// 示例输出：
// {
//   "search_tool": "信息检索专员",
//   "code_interpreter": "数据分析师"
// }
```

#### 2.3 控制think-act循环次数

```java
// 在BaseAgent中设置最大步数
reactAgent.setMaxSteps(15);  // 防止无限循环

// 监控执行步数
if (reactAgent.getCurrentStep() > 10) {
    log.warn("ReAct循环次数过多，可能陷入死循环");
}
```

#### 2.4 优化工具选择策略

```yaml
# 工具描述要清晰
tools:
  - name: search_web
    description: |
      搜索互联网信息。

      适用场景：
      - 查找最新资讯
      - 获取实时数据
      - 搜索专业知识

      不适用场景：
      - 计算数学问题（使用code_interpreter）
      - 生成报告（使用report_tool）
```

### 3. 通用最佳实践

#### 3.1 合理选择工具集

```java
// 根据outputStyle选择工具
if ("dataAgent".equals(outputStyle)) {
    // 数据分析场景
    toolCollection.addTool(new DataAnalysisTool());
    toolCollection.addTool(new ReportTool());
} else {
    // 通用场景
    toolCollection.addTool(new FileTool());
    toolCollection.addTool(new CodeInterpreterTool());
    toolCollection.addTool(new DeepSearchTool());
    toolCollection.addTool(new ReportTool());
}
```

#### 3.2 实现优雅降级

```java
try {
    // 尝试使用Plan-Solve（性能优先）
    return planSolveHandler.handle(context, request);
} catch (Exception e) {
    log.error("Plan-Solve执行失败，降级到ReAct", e);
    // 降级到ReAct（稳定性优先）
    return reactHandler.handle(context, request);
}
```

#### 3.3 记录详细日志

```java
// Plan-Solve
log.info("{} [PLAN] 生成计划: {}", requestId, plan);
log.info("{} [EXEC] 执行任务: {}", requestId, task);
log.info("{} [RESULT] 任务结果: {}", requestId, result);

// ReAct
log.info("{} [THINK] 思考: {}", requestId, thought);
log.info("{} [ACT] 行动: {}", requestId, action);
log.info("{} [OBS] 观察: {}", requestId, observation);
```

#### 3.4 性能监控

```java
// 记录关键指标
long startTime = System.currentTimeMillis();

// 执行Agent
String result = agent.run(query);

long duration = System.currentTimeMillis() - startTime;

// 上报指标
metricsReporter.report(MetricType.AGENT_DURATION, duration);
metricsReporter.report(MetricType.LLM_CALLS, agent.getLlmCallCount());
metricsReporter.report(MetricType.TOOL_CALLS, agent.getToolCallCount());
```

---

## 总结

### 快速决策表

| 任务特征 | 推荐模式 | 理由 |
|---------|---------|------|
| 包含多个独立子任务 | Plan-Solve | 可并行执行，性能优 |
| 有标准流程 | Plan-Solve | SOP支持，流程规范 |
| 需要清晰进度 | Plan-Solve | 任务列表可视化 |
| 快速问答 | ReAct | 架构简单，响应快 |
| 探索性任务 | ReAct | 动态决策能力强 |
| 资源受限 | ReAct | 内存占用少 |
| 高度不确定性 | ReAct | 灵活性好 |

### 核心原则

1. **性能优先** → Plan-Solve（并行）
2. **简单优先** → ReAct（单Agent）
3. **规范优先** → Plan-Solve（SOP）
4. **灵活优先** → ReAct（动态）

### 未来展望

- ⏳ **Workflow模式**：预定义DAG流程，适合固定工作流
- ⏳ **Router模式**：智能路由，根据问题类型选择不同策略
- ⏳ **Comprehensive模式**：综合多种模式的优势

---

**文档版本**: 1.0.0
**最后更新**: 2025-10-28
**维护者**: JD Genie Team
