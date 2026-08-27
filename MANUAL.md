# Librax 使用手册

> **版本：** 2025.11-SNAPSHOT  
> **适用对象：** 开发者、实施工程师、实验室运维人员

---

## 目录

1. [平台概览](#1-平台概览)
2. [样本管理](#2-样本管理)
3. [流程/流水线](#3-流程流水线)
4. [资源调度](#4-资源调度)
5. [设备管理](#5-设备管理)
6. [任务系统](#6-任务系统)
7. [部署指南](#7-部署指南)
8. [派生项目](#8-派生项目)
9. [发布底座库](#9-发布底座库)
10. [常见问题](#10-常见问题)

---

## 1. 平台概览

Librax 是基于 [RuoYi-Vue-Pro](https://gitee.com/zhijiantianya/ruoyi-vue-pro) 构建的实验室自动化平台，在原框架的系统管理、基础设施之上，增加了以下专属模块：

| 模块                       | 职责                 |
| ------------------------ | ------------------ |
| `librax-module-lab`      | 样本全生命周期管理、物料/耗材    |
| `librax-module-flow`     | 声明式流水线引擎（YAML/DAG） |
| `librax-module-device`   | 仪器设备通信与协议适配        |
| `librax-module-resource` | 资源分区、工位分配、分布式锁     |
| `librax-module-task`     | 统一任务协议（解耦执行实现）     |
| `librax-module-ai`       | 大模型集成（对话/知识库/工作流）  |

### 整体架构

```
前端 (Vue3 / Vben)
        │
   REST API 层
        │
┌───────────────────────────────────────────┐
│            业务服务层                      │
│  样本管理  流水线引擎  设备通信  资源调度  │
└───────────────────────────────────────────┘
        │
┌───────────────────────────────────────────┐
│       基础设施 (RuoYi-Vue-Pro)            │
│  安全/Auth  缓存  日志  代码生成  权限    │
└───────────────────────────────────────────┘
        │
  MySQL  Redis  RabbitMQ  设备(TCP/HTTP/串口)
```

---

## 2. 样本管理

### 2.1 核心字段说明

| 字段               | 含义               | 示例                                    |
| ---------------- | ---------------- | ------------------------------------- |
| `sampleId`       | 样本唯一业务编号（条码/二维码） | `SMP-20250601-001`                    |
| `sampleType`     | 样本类型             | BLOOD / URINE / WATER / SOIL          |
| `containerType`  | 容器类型             | TUBE / PLATE / VIAL / BAG             |
| `volumeUl`       | 当前体积（微升）         | `500`                                 |
| `priority`       | 优先级              | 0=普通 1=紧急 2=危急                        |
| `source`         | 来源               | MANUAL / IMPORT / LIS / DEVICE        |
| `status`         | 当前状态（见下方状态机）     | `IN_PROCESS`                          |
| `parentSampleId` | 父样本（派生时填写）       | `SMP-20250601-001`                    |
| `deriveType`     | 派生方式             | SPLIT / MERGE / ALIQUOT / ADD_REAGENT |

### 2.2 样本状态机

```
REGISTERED（已登记）
    │ 装载
    ▼
LOADED（已装载）
    │ 流程启动
    ▼
IN_PROCESS（检测中）
    │
    ├──→ SPLIT（已拆分）   ← 执行 SAMPLE_SPLIT 步骤后
    │
    ▼
COMPLETED（已完成）
    │
    ├──→ ARCHIVED（已归档）
    ├──→ REJECTED（不合格）
    └──→ LOST（丢失）
```

### 2.3 登记样本（步骤）

1. **手工登记**：进入「样本管理 → 新建」，填写 sampleId、类型、体积、优先级
2. **批量导入**：上传 Excel，系统自动映射字段并批量写入
3. **LIS 接入**：外部系统调用 `POST /lab/sample-info/create`，传入 `source=LIS`、`externalId`
4. **设备推送**：流水线 SAMPLE_SPLIT 步骤自动生成子样本，`deriveType=SPLIT`

### 2.4 样本谱系

样本拆分后会生成 `lab_sample_relation` 记录，字段 `parentSampleId / rootSampleId / generation` 共同构成谱系树。可通过「样本详情 → 谱系」查看完整的亲代/子代关系。

### 2.5 物料/耗材

| 实体                    | 含义               |
| --------------------- | ---------------- |
| `MaterialDef`         | 物料主数据（名称、规格）     |
| `MaterialInstance`    | 库存实例（批次、数量、过期时间） |
| `MaterialConsumption` | 消耗记录             |
| `MaterialCheckRule`   | 质控规则（入库/使用前校验）   |

物料在流水线 INSTRUMENT 步骤执行时自动扣减，也可手工在「物料管理」中调整。

---

## 3. 流程/流水线

### 3.1 核心概念

| 概念                     | 说明                 |
| ---------------------- | ------------------ |
| **PipelineDefinition** | 流程模板（YAML 定义或界面配置） |
| **PipelineStep**       | 步骤节点（DAG 图中的节点）    |
| **StepDefinition**     | 步骤组件库（可复用的步骤模板）    |
| **PipelineExecution**  | 流程运行实例             |
| **StepExecution**      | 单步运行记录             |
| **ExecutionContext**   | 运行时变量存储（步骤间传值）     |

### 3.2 步骤类型

| 类型              | 说明             | 关键参数                                    |
| --------------- | -------------- | --------------------------------------- |
| `INSTRUMENT`    | 调用仪器设备         | `resource.type`, `commandCode`          |
| `COMPUTE`       | 调用 Spring Bean | `bean`                                  |
| `CONDITION`     | 条件分支           | `condition.expr`, `true_to`, `false_to` |
| `MANUAL`        | 人工干预/审批        | `assignee`                              |
| `WAIT`          | 延时等待           | `duration_ms`                           |
| `UNIT_LAUNCHER` | 启动子流程          | `unit_key`                              |
| `SAMPLE_SPLIT`  | 样本拆分           | `split_count`, `volume_each_ul`         |
| `NOTIFY`        | 通知（邮件/Webhook） | `channel`, `template`                   |

### 3.3 YAML 流水线定义

完整的 YAML 结构示例（水质检测）：

```yaml
pipeline:
  key: water_quality_test       # 唯一标识（小写下划线）
  version: 1
  name: 水质检测
  zone: ZONE-A                  # 默认资源分区
  timeout_ms: 1800000           # 全局超时 30 分钟
  on_failure: FAIL_FAST         # 失败策略：FAIL_FAST / SKIP / CONTINUE_ON_FAIL

steps:
  - id: s_load                  # 步骤 ID（全流程唯一）
    type: INSTRUMENT
    step_key: sample_loader     # 引用 StepDefinition
    resource:
      type: LOADER
      zone: ZONE-A
    output_mapping:
      sample_id: $.data.sampleId   # JSONPath 提取输出

  - id: s_ph
    type: INSTRUMENT
    depends_on: [s_load]        # DAG 依赖
    resource:
      type: PH_METER
    input_mapping:
      sample_id: ${s_load.sample_id}   # 引用上游步骤输出
    output_mapping:
      ph_value: $.data.phValue
    max_attempts: 3             # 失败重试次数
    backoff_ms: 2000

  - id: s_calc
    type: COMPUTE
    depends_on: [s_ph]
    bean: waterQualityCalcBean  # Spring Bean 名称
    input_mapping:
      ph: ${s_ph.ph_value}
    output_mapping:
      score: $.score

  - id: s_judge
    type: CONDITION
    depends_on: [s_calc]
    condition:
      expr: "${s_calc.score} >= 80"   # Aviator 表达式
      true_to: s_archive
      false_to: s_notify

  - id: s_archive
    type: COMPUTE
    depends_on: [s_judge]
    bean: archiveResultBean

  - id: s_notify
    type: NOTIFY
    depends_on: [s_judge]
    channel: WEBHOOK
    template: "样本 ${sample_id} 检测不合格，分数 ${s_calc.score}"
```

### 3.4 导入 YAML 流程

```
1. 准备 YAML 文件（参考 pipeline-schema.json 格式校验）
2. 调用 POST /flow/pipeline-import/yaml
   或进入「流程管理 → 导入」上传文件
3. 系统解析后生成 PipelineDefinition + PipelineStep 记录
4. 状态默认 DRAFT，确认后改为 ACTIVE
```

### 3.5 启动流程执行

```
POST /flow/pipeline-execution/create
{
  "pipelineKey": "water_quality_test",
  "version": 1,
  "sampleId": "SMP-20250601-001",
  "inputParams": { "operator": "张三" }
}
```

### 3.6 执行流程（内部）

```
创建 PipelineExecution 实例
        │
   DAG 解析 → 找出首批无依赖步骤
        │
   对每个就绪步骤：
    ├── 检查资源可用性（ZoneQuota + Redis 锁）
    ├── 分配资源（StepResourceHold）
    ├── 创建 Task 实例 → 提交执行
    │     INSTRUMENT → 调用 DeviceCommand
    │     COMPUTE    → 调用 Spring Bean
    │     CONDITION  → 计算 Aviator 表达式
    │     MANUAL     → 等待人工操作
    ├── JSONPath 解析结果 → 写入 ExecutionContext
    ├── 释放资源锁
    └── 触发后续步骤
        │
  全部完成 → PipelineExecution.status = COMPLETED
```

### 3.7 单步调试

```
POST /flow/standalone/run-step
{
  "pipelineKey": "water_quality_test",
  "stepId": "s_ph",
  "contextOverride": { "s_load.sample_id": "TEST-001" }
}
```

支持跳过依赖，直接运行指定步骤；方便开发阶段验证单个设备调用。

### 3.8 监控与查看

- **实时状态**：`GET /flow/execution-monitor/{executionId}`
- **步骤日志**：`GET /flow/step-execution/list?executionId=xxx`
- **事件流水**：`GET /flow/pipeline-event-log/list?executionId=xxx`

---

## 4. 资源调度

### 4.1 概念层次

```
ZoneQuota（分区容量）
  └── ResourceConfig（资源类型配置，如 PH_METER、LOADER）
        └── RackInfo（机架/工位台）
              └── SlotInfo（工位槽，1 槽 = 1 台仪器）
```

### 4.2 配置步骤

**第一步：创建分区容量**

```
POST /resource/zone-quota/create
{
  "zone": "ZONE-A",
  "resourceType": "PH_METER",
  "total": 4          # 该分区最多同时运行 4 台 PH 计
}
```

**第二步：注册设备资源**

```
POST /resource/resource-config/create
{
  "resourceType": "PH_METER",
  "zone": "ZONE-A",
  "capacity": 1,
  "deviceId": "PH-METER-01"
}
```

**第三步：配置机架（可选）**

进入「资源管理 → 机架」，定义实验室物理拓扑（行数、列数），再将设备分配到具体槽位。

### 4.3 流水线步骤声明资源需求

在 YAML 步骤中声明：

```yaml
- id: s_ph
  type: INSTRUMENT
  resource:
    type: PH_METER     # 匹配 ResourceConfig.resourceType
    zone: ZONE-A       # 可选，不填则全区抢占
    quantity: 1        # 默认 1
```

引擎在执行该步骤前自动通过 Redis 分布式锁竞争资源，执行完成后释放。

---

## 5. 设备管理

### 5.1 设备信息（DeviceInfo）

| 字段                      | 说明                                      |
| ----------------------- | --------------------------------------- |
| `deviceId`              | 设备业务编号（如 `PH-METER-01`）                 |
| `deviceType`            | 类型（PH_METER / BALANCE / TITRATOR / AGV） |
| `protocol`              | 通信协议（TCP / HTTP / SERIAL / SDK / MOCK）  |
| `host / port`           | TCP/HTTP 连接地址                           |
| `serialPort / baudRate` | 串口参数                                    |
| `callbackMode`          | 结果获取方式：WEBHOOK（设备主动推送）/ POLL（轮询）        |
| `authType`              | 认证：NONE / BASIC / TOKEN / HMAC          |
| `maxConcurrent`         | 最大并发指令数                                 |
| `heartbeatIntervalMs`   | 心跳间隔（ms），0 表示不启用心跳                      |

### 5.2 注册设备（步骤）

```
1. 进入「设备管理 → 设备列表 → 新建」
2. 填写 deviceId、类型、协议、连接参数
3. 保存后点击「测试连接」验证可达性
4. 状态变为 ONLINE 即表示注册成功
```

### 5.3 设备指令（DeviceCommand）

每台设备可配置多条指令，对应不同操作：

| 字段                      | 说明                                 |
| ----------------------- | ---------------------------------- |
| `commandCode`           | 指令码（MEASURE / COLLECT / CALIBRATE） |
| `requestTemplate`       | 请求模板，支持 `${param}` 占位符             |
| `httpMethod / httpPath` | HTTP 设备专用                          |
| `contentType`           | 串口/TCP 内容格式：HEX / ASCII / BINARY   |
| `codecId`               | 响应解析器 ID                           |
| `retryable`             | 是否自动重试                             |
| `timeoutMs`             | 单次超时                               |
| `pollPath`              | 异步轮询 URL（callbackMode=POLL 时）      |
| `pollDoneExpr`          | 轮询完成条件（JSONPath 表达式）               |
| `pollMaxTimes`          | 最大轮询次数                             |

### 5.4 响应解析器（DeviceCodec）

设备返回的原始数据（JSON / HEX / 二进制）通过 Codec 解析为结构化结果。配置规则：

- `codecType`：JSON_PATH / REGEX / BINARY_STRUCT
- `extractRules`：字段名 → 提取表达式的映射列表

### 5.5 直接调用设备（不走流水线）

适合调试或临时操作：

```
POST /device/device-command/execute
{
  "deviceId": "PH-METER-01",
  "commandCode": "CALIBRATE",
  "params": { "offset": 0.5 }
}
→ 返回 { "execId": "exec-xxx" }

GET /device/device-command/execute/result?execId=exec-xxx
→ 返回执行状态和结果
```

结果缓存 2 小时，可重复查询。

### 5.6 设备回调（WEBHOOK 模式）

设备执行完成后主动 POST 到：

```
POST /device/device-callback/{deviceId}/{commandCode}
```

系统收到后更新对应 StepExecution 并继续流水线。

---

## 6. 任务系统

任务模块是流水线引擎与具体执行单元之间的解耦层。

### 6.1 任务状态

```
PENDING → RUNNING → SUCCESS
                  → FAILED
                  → TIMEOUT
```

### 6.2 执行器配置（TaskExecutorConfig）

| 执行器类型 | 适用场景               |
| ----- | ------------------ |
| 本地线程池 | 普通计算/简单设备调用        |
| 消息队列  | 高并发、异步分发给远端 Worker |
| 外部服务  | 调用微服务/第三方 API      |
| 人工队列  | MANUAL 步骤，等待人工完成   |

配置入口：「任务管理 → 执行器配置」。

### 6.3 外部 Worker 回调

远端执行器完成后回调：

```
POST /task/task-callback/{taskId}
{
  "status": "SUCCESS",
  "result": { ... }
}
```

---

## 7. 部署指南

### 7.1 前置依赖

| 依赖     | 最低版本    | 说明                  |
| ------ | ------- | ------------------- |
| JDK    | 17 / 21 | Spring Boot 3.2+ 要求 |
| MySQL  | 5.7+    | 主数据库                |
| Redis  | 6.0+    | 缓存 + 分布式锁           |
| Maven  | 3.8+    | 构建工具                |
| Docker | 20+     | 容器部署（可选）            |

### 7.2 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE librax CHARACTER SET utf8mb4;"

# 导入基础表结构（RuoYi + Librax 全量）
mysql -u root -p librax < sql/mysql/ruoyi-vue-pro.sql

# 导入测试数据（可选，含设备/流程示例数据）
mysql -u root -p librax < sql/test/device_flow_test_data.sql
```

### 7.3 本地开发启动

1. **配置数据源**（`librax-server/src/main/resources/application-local.yaml`）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/librax?...
    username: root
    password: yourpassword
  redis:
    host: localhost
    port: 6379
```

2. **Maven 构建**

```bash
# 使用项目自定义 Maven（如已配置）
mvn clean package -DskipTests -pl librax-server -am
```

3. **启动**

```bash
java -jar librax-server/target/librax-server.jar --spring.profiles.active=local
```

4. 访问 `http://localhost:48080`，默认账号 `admin / admin123`

### 7.4 一键部署到服务器

脚本 `deploy-to-server.sh` 执行完整流程：本地打包 → SCP 上传 → 服务器构建 Docker 镜像 → 启动容器。

**部署测试环境：**

```bash
bash deploy-to-server.sh
# 默认目标：192.168.42.228（在脚本顶部 DEV_SERVER_HOST 配置）
```

**部署生产环境：**

1. 编辑 `deploy-to-server.sh`，填写生产服务器配置：

```bash
PROD_SERVER_HOST="x.x.x.x"     # 客户服务器 IP
PROD_SERVER_USER="root"
PROD_SERVER_PASS="changeme"

PROD_DB_HOST="mysql-local"
PROD_DB_NAME="librax-sz"        # 数据库名
PROD_DB_PASS="strongpassword"   # 务必修改！
```

2. 执行：

```bash
bash deploy-to-server.sh prod
```

脚本会自动等待服务健康检查通过后输出启动完成。

### 7.5 部署流程图

```
本地
  ├── mvn clean package -DskipTests
  └── SCP jar → 服务器 /opt/app/librax-server.jar
                      │
               服务器（Docker）
                      ├── docker build -t librax-server .
                      ├── docker stop / rm librax-server（旧容器）
                      └── docker run -d \
                            -p 48080:48080 \
                            -e DB_HOST=... \
                            -e REDIS_HOST=... \
                            librax-server
                                  │
                          轮询 /actuator/health
                                  │
                          HEALTHY → 完成
```

### 7.6 日志

- 应用日志：`/opt/logs/librax-server/`
- 建议配合 Promtail + Loki + Grafana 做日志聚合

---

## 8. 派生项目

基于 Librax 底座快速创建新业务项目，使用 `create-project.sh`。

### 8.1 创建项目骨架

```bash
./create-project.sh \
  --name deep-principle \          # 项目名（kebab-case）
  --group com.megarobo \           # Maven groupId
  --librax-version 2025.11 \       # 底座版本（默认 SNAPSHOT）
  --port 48090 \                   # 服务端口（默认 48080）
  --output /Users/yinan/workspace/megarobo
```

### 8.2 生成的目录结构

```
deep-principle/
├── pom.xml                              # 父 POM，引入 librax-dependencies BOM
├── deep-principle-server/
│   ├── pom.xml                          # 依赖 librax-module-system/infra/resource/flow
│   └── src/main/
│       ├── java/.../DeepPrincipleApplication.java
│       └── resources/
│           ├── application.yaml
│           └── application-local.yaml   # ← 填写 DB/Redis 连接
└── deep-principle-module-biz/
    ├── pom.xml                          # 依赖 librax-module-flow-api / device-api
    └── src/main/java/.../module/biz/    # ← 编写业务逻辑
```

### 8.3 开发流程

```
1. 配置 application-local.yaml（DB / Redis）
2. 在 module-biz 中编写业务 Service / Controller
3. 在「流程管理」中定义或导入 YAML 流水线
4. 在「设备管理」中注册仪器
5. 在「资源管理」中配置分区和工位
6. 通过单步调试验证每个设备步骤
7. 端到端跑完整流水线
```

### 8.4 引用 Librax 模块

在 `deep-principle-module-biz/pom.xml` 中按需引入：

```xml
<!-- 流程 API（创建/查询执行） -->
<dependency>
  <groupId>com.librax</groupId>
  <artifactId>librax-module-flow-api</artifactId>
</dependency>

<!-- 设备 API（直接调用设备） -->
<dependency>
  <groupId>com.librax</groupId>
  <artifactId>librax-module-device-api</artifactId>
</dependency>

<!-- 样本服务（需要直接操作样本时） -->
<dependency>
  <groupId>com.librax</groupId>
  <artifactId>librax-module-lab</artifactId>
</dependency>
```

### 8.5 自定义 COMPUTE 步骤

实现 Spring Bean 供 COMPUTE 步骤调用：

```java
@Component("waterQualityCalcBean")   // Bean 名称对应 YAML 中的 bean 字段
public class WaterQualityCalcBean implements StepExecutorBean {

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        double ph = (double) input.get("ph");
        double score = ph >= 6.5 && ph <= 8.5 ? 90.0 : 40.0;
        return Map.of("score", score);
    }
}
```

---

## 9. 发布底座库

当 Librax 底座代码有更新，需发布到 Maven 仓库供派生项目使用。

### 9.1 发布 SNAPSHOT

```bash
./deploy-librax.sh
```

自动执行：

1. 读取当前 `pom.xml` 中的 SNAPSHOT 版本
2. `mvn deploy` 发布到云效制品仓库（4 线程并行）
3. 同步到 `~/.m2/repository`（供 IntelliJ 派生项目 IDE 感知）
4. 清除 Maven 失败解析缓存

### 9.2 发布正式版本

```bash
./deploy-librax.sh 2025.11
```

脚本会先通过 `versions:set` 将所有模块的版本号修改为 `2025.11`，再执行发布。

### 9.3 发布模块列表

每次发布包含以下模块：

```
librax-dependencies          # BOM 版本管理
librax-framework             # 框架扩展（Security/Redis/MyBatis 等 starter）
librax-module-system         # 系统管理
librax-module-infra          # 基础设施
librax-module-resource       # 资源调度
librax-module-flow           # 流水线引擎（含 API）
librax-module-device         # 设备管理（含 API）
librax-module-lab            # 样本管理
librax-module-task           # 任务系统
```

### 9.4 派生项目升级版本

修改派生项目 `pom.xml` 中的版本号：

```xml
<properties>
  <librax.version>2025.11</librax.version>
</properties>
```

然后 `mvn clean install` 或在 IDE 中刷新依赖。

---

## 10. 常见问题

### Q1：流水线步骤一直处于 PENDING，不执行？

**原因：** 资源不可用，Redis 锁被其他执行占用，或 `ZoneQuota` 容量已满。

**排查：**

```
1. 查看 lab_step_resource_hold 表，检查是否有未释放的持有记录
2. 检查 Redis 中 resource:lock:* 键是否存在异常过期时间
3. 查看 ZoneQuota.current 是否等于 total
```

**处理：** 手动清理僵死的 Hold 记录，或扩大 ZoneQuota 容量。

---

### Q2：设备调用超时 / 状态一直 RUNNING？

**排查步骤：**

```
1. 检查设备网络连通性（ping / telnet host port）
2. 查看 DeviceInfo.status 是否为 OFFLINE
3. 检查 DeviceCommand.timeoutMs 是否合理
4. 若 callbackMode=WEBHOOK，确认设备能访问到 Librax 服务的回调地址
5. 若 callbackMode=POLL，检查 pollPath 和 pollDoneExpr 配置是否正确
```

---

### Q3：YAML 导入报错？

**常见原因：**

- `pipelineKey` 已存在且状态为 ACTIVE
- `depends_on` 引用了不存在的步骤 ID
- `step_key` 未在 StepDefinition 中注册
- 条件步骤缺少 `true_to` 或 `false_to`

可用 `pipeline-schema.json` 在本地提前做 JSON Schema 校验：

```bash
# 使用 ajv-cli 校验（需 Node.js）
npx ajv validate -s pipeline-schema.json -d your-pipeline.yaml
```

---

### Q4：派生项目构建时找不到 librax 依赖？

**排查：**

1. 确认已执行 `./deploy-librax.sh` 且成功
2. 检查派生项目 `settings.xml` 中是否配置了云效仓库地址
3. 本地开发可直接使用 `~/.m2/repository` 中的版本（脚本已自动同步）

---

### Q5：如何增加新的设备类型？

1. 在「设备管理 → 设备类型」中新增类型枚举
2. 注册对应的 `DeviceInfo`（填写连接参数）
3. 为该设备类型创建 `DeviceCommand`（指令模板）
4. 配置 `DeviceCodec`（响应解析规则）
5. 在 `ResourceConfig` 中注册为可调度资源
6. 在流水线 YAML 的 `resource.type` 中引用

---

*最后更新：2026-06-06*
