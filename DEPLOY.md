# 演唱会抢票秒杀系统 - 云原生与全栈一体化部署指南

> 本项目已完成前后端一体化容器配置，支持以 **100% 永久免费（$0）** 方案一键上线至 Render 容器平台，分配独立二级域名（例如 `https://flash-sale.onrender.com`），供简历项目展示与面试官在线体验。

---

## 架构拓扑与零成本选型

```
┌──────────────────┐               ┌─────────────────────────────────┐
│  访客 / 面试官    ├──────────────►│   Render Cloud (Free Tier)      │
│  Browser / HTTPS │ 专属免费域名    │   • 0.1 CPU / 512 MB RAM        │
└──────────────────┘               │   • Alpine JRE 17 容器          │
                                   │   • Vue 3 前端静态托管 + SpringBoot│
                                   └────────┬───────────────┬────────┘
                                            │               │
                                 JDBC / TLS │               │ Redis / RabbitMQ
                                            ▼               ▼
                                 ┌────────────────┐ ┌────────────────┐
                                 │  TiDB Cloud    │ │ Upstash Redis  │
                                 │  Serverless    │ │ + CloudAMQP    │
                                 │  (免费 MySQL)  │ │ (免费队列与缓存)│
                                 └────────────────┘ └────────────────┘
```

| 组件 | 服务商与选型 | 费用 | 说明 |
| :--- | :--- | :--- | :--- |
| **代码托管** | **GitHub** (`Konglong7/concert-flash-sale`) | 免费 | 触发 Render 自动化 CI/CD 构建 |
| **应用容器** | **Render Web Service** | 免费 (512MB RAM) | 多阶段 Dockerfile 自动构建 Vue3 + Spring Boot 3 |
| **MySQL** | **TiDB Cloud Serverless** | 免费 (5GB) | 100% 兼容 MySQL 8.0，执行 `backend/sql/schema.sql` |
| **Redis** | **Upstash Redis** | 免费 (10k 请求/天) | 原生支持 TLS，用于热点预扣与 Lua 原子限流 |
| **RabbitMQ** | **CloudAMQP** (Lemur plan) | 免费 (100万消息/月) | 免运维云端 RabbitMQ 实例 |

---

## 极速上线流程 (约 3 分钟)

### 步骤 1：云端数据库与中间件就绪（如已有可直接复用）

1. **MySQL (TiDB Cloud)**：
   - 登录 [TiDB Cloud](https://tidbcloud.com/)，在 SQL Editor 中执行项目自带的建表脚本：
     - [`backend/sql/schema.sql`](./backend/sql/schema.sql)（创建演出、票档、订单、用户表）
     - [`backend/sql/seed.sql`](./backend/sql/seed.sql)（初始化热门演唱会与票档数据）
2. **Redis (Upstash)**：
   - 在 [Upstash Console](https://console.upstash.com/) 创建免费 Redis 数据库，获取 Endpoint、Port 与 Password。
3. **RabbitMQ (CloudAMQP)**：
   - 在 [CloudAMQP](https://customer.cloudamqp.com/) 创建免费 Lemur 实例，获取 Host、Username、Password。

### 步骤 2：在 Render 创建 Web Service

1. 登录 [Render Dashboard](https://dashboard.render.com/)，点击 **New +** -> **Web Service**；
2. 连接 GitHub 仓库 `Konglong7/concert-flash-sale`；
3. 配置基本信息：
   - **Name**：`flash-sale`（自动分配 `https://flash-sale.onrender.com`）
   - **Region**：推荐 `Oregon (US West)`
   - **Runtime**：`Docker`
   - **Instance Type**：`Free` (512 MB RAM / 0.1 CPU)
4. 配置环境变量（Environment Variables）：
   - `MYSQL_HOST`：云端 MySQL 地址
   - `MYSQL_PORT`：端口（如 4000 或 3306）
   - `MYSQL_USER`：数据库用户名
   - `MYSQL_PASSWORD`：数据库密码
   - `REDIS_HOST`：云端 Redis 地址
   - `REDIS_PORT`：云端 Redis 端口
   - `REDIS_PASSWORD`：云端 Redis 密码
   - `RABBITMQ_HOST`：CloudAMQP 主机名
   - `RABBITMQ_USER`：CloudAMQP 用户名
   - `RABBITMQ_PASS`：CloudAMQP 密码
5. 点击 **Create Web Service**，Render 将自动执行 Dockerfile 的三阶段构建并在数分钟内上线完成！
