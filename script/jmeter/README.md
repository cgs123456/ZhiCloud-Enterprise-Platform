# Yudao 性能压测脚本

芋道源码（yudao）项目的性能压测方案，提供 **JMeter** 与 **Gatling** 两种实现。

## 目录结构

```
script/
├── jmeter/
│   ├── yudao-load-test.jmx      # JMeter 测试计划
│   └── README.md                # 本文件
├── gatling/
│   └── src/test/scala/yudao/
│       └── YudaoLoadTest.scala  # Gatling 压测脚本
└── docker/
    └── grafana/
        └── dashboards/
            └── grafana-dashboard-performance.json   # 性能监控看板
```

## 压测前置条件

### 1. 环境准备

| 项目 | 要求 | 说明 |
| --- | --- | --- |
| yudao-server | 已启动且健康 | 业务端口 `48080`，管理端口 `48090` |
| 数据库 | MySQL 8.x，已初始化数据 | 执行 `sql/mysql/zhicloud_platform.sql` |
| Redis | 6.x 及以上 | 默认 `localhost:6379` |
| 测试账号 | `admin / admin123` | 租户 ID `1` |
| JVM | 堆内存建议 `-Xms2g -Xmx2g` | 启动参数见 `yudao-server-startup.out` |

### 2. 监控就绪

- Prometheus 已采集 yudao-server 的 `48090` actuator 指标
- Grafana 已导入 `grafana-dashboard-performance.json` 看板
- 数据库、Redis、宿主机 CPU/内存监控就绪

### 3. 压测机配置

- JMeter：JDK 11+，JMeter 5.5+，建议堆内存 `-Xms2g -Xmx4g`
- Gatling：JDK 11+，Maven 3.6+，Scala 2.13
- 网络到被测服务器单程延迟 < 5ms（建议同机房）

### 4. 注意事项

- 压测前请关闭验证码校验或确认 `captchaVerification` 可为空
- 建议在**预发布/测试环境**执行，避免影响生产
- 单台压测机并发超过 500 时，建议采用 JMeter 分布式压测
- 登录接口需先通过 `setUp` 线程组获取全局 `accessToken`，供后续鉴权场景使用

---

## JMeter 执行命令

### GUI 模式（仅调试用）

```bash
# Windows
jmeter -t script/jmeter/yudao-load-test.jmx

# Linux / macOS
jmeter -t script/jmeter/yudao-load-test.jmx
```

> ⚠️ GUI 模式会消耗额外资源，**正式压测请使用非 GUI 模式**。

### 非 GUI 模式（正式压测）

```bash
jmeter -n -t script/jmeter/yudao-load-test.jmx \
  -l result/yudao-result.jtl \
  -e -o result/yudao-report \
  -JHOST=localhost -JPORT=48080 -JTENANT_ID=1
```

参数说明：

| 参数 | 说明 |
| --- | --- |
| `-n` | 非 GUI 模式 |
| `-t` | 测试计划文件路径 |
| `-l` | 结果文件（.jtl）输出路径 |
| `-e` | 测试结束后生成 HTML 报告 |
| `-o` | HTML 报告输出目录（必须为空目录） |
| `-JXXX` | 覆盖用户定义变量（HOST/PORT/TENANT_ID/USERNAME/PASSWORD） |

### 分布式压测

```bash
# server 端启动（多台压测机）
jmeter-server

# master 端执行
jmeter -n -t yudao-load-test.jmx -R 192.168.1.101,192.168.1.102 \
  -l result/yudao-result.jtl -e -o result/yudao-report
```

### 生成 HTML 报告（基于已有结果文件）

```bash
jmeter -g result/yudao-result.jtl -o result/yudao-report
```

---

## Gatling 执行命令

### 前置：Gatling 项目构建配置

`script/gatling/pom.xml` 需引入 `gatling-maven-plugin`（如已配置可跳过）：

```xml
<dependency>
  <groupId>io.gatling.highcharts</groupId>
  <artifactId>gatling-charts-highcharts</artifactId>
  <version>3.9.5</version>
  <scope>test</scope>
</dependency>
```

### 执行压测

```bash
# 方式一：Maven 运行指定 Simulation
mvn gatling:test -Dgatling.simulationClass=yudao.YudaoLoadTest

# 方式二：仅编译，不执行
mvn test-compile

# 方式三：Gatling CLI 直接运行
gatling.sh -sf script/gatling/src/test/scala -rs yudao.YudaoLoadTest
```

报告默认输出到 `target/gatling/` 目录，打开 `index.html` 即可查看。

---

## 测试场景一览

| 编号 | 场景 | 接口 | 方法 | 并发数 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 0 | 全局登录 | `/admin-api/system/auth/login` | POST | 1 | setUp 获取全局 Token |
| 1 | 登录场景 | `/admin-api/system/auth/login` | POST | 100 | 提取 accessToken |
| 2 | 用户列表查询 | `/admin-api/system/user/page` | GET | 200 | Bearer Token |
| 3 | 字典数据查询 | `/admin-api/system/dict-type/list-all-simple` | GET | 300 | 高并发只读 |
| 4 | AI 对话 | `/admin-api/ai/chat/conversation/send-message` | POST | 50 | 响应较慢 |
| 5 | ERP 产品列表 | `/admin-api/erp/product/product/page` | GET | 100 | Bearer Token |

**全局配置**：总并发 300、Ramp-up 30s、Loop 10、Duration 300s。

---

## 结果指标说明

### JMeter 指标

| 指标 | 含义 | 查看位置 |
| --- | --- | --- |
| `Average` | 平均响应时间（ms） | Summary / Aggregate Report |
| `Min` / `Max` | 最小 / 最大响应时间（ms） | Aggregate Report |
| `90% Line` / `95% Line` / `99% Line` | 百分位响应时间（ms） | Aggregate Report |
| `Throughput` | 吞吐量（TPS，req/s） | Aggregate Report |
| `Received KB/sec` | 每秒接收数据量 | Aggregate Report |
| `Error %` | 错误率 | Summary / Aggregate Report |
| `Active` | 活跃线程数 | Response Time Graph |

### HTML 报告关键图表

- **Statistics**：各接口统计汇总
- **Response Times Over Time**：响应时间趋势
- **Throughput**：吞吐量趋势
- **Active Threads Over Time**：活跃线程趋势
- **Errors**：错误分布

---

## 上线判定标准

### 核心指标阈值

| 指标 | 标准（Web API） | 说明 |
| --- | --- | --- |
| P95 响应时间 | < 500 ms | 95% 请求响应时间 |
| P99 响应时间 | < 1000 ms | 99% 请求响应时间 |
| 错误率 | < 0.5% | 非业务错误（5xx、超时、断言失败） |
| TPS | > 500 | 系统每秒事务数 |
| CPU 使用率 | < 80% | 服务器 CPU 平均使用率 |
| 内存使用率 | < 80% | JVM 堆 + 堆外内存使用率 |

### 判定逻辑

```
通过 = P95 < 500ms
   AND P99 < 1000ms
   AND 错误率 < 0.5%
   AND TPS > 500
   AND CPU < 80%
   AND 内存 < 80%

任一指标不达标 → 需排查并优化后复测
```

### 补充关注项

- **Druid 连接池**：`active` 数 < `maxActive`，无等待
- **GC**：Full GC 频率 < 1 次/分钟，单次 STW < 200ms
- **数据库慢查询**：无超过 1s 的慢 SQL
- **Redis**：无阻塞、内存稳定
- **线程池**：无大量线程 BLOCKED / WAITING

---

## 结果归档建议

每次压测完成后，建议归档以下内容：

1. JMeter HTML 报告目录（`result/yudao-report/`）
2. Gatling 报告目录（`target/gatling/`）
3. Grafana 看板截图（压测期间快照）
4. 服务器 CPU/内存/GC 监控截图
5. 压测结论与优化建议文档

命名规范：`YYYYMMDD-场景-版本号`，例如 `20260729-full-load-v1.0.0`。
