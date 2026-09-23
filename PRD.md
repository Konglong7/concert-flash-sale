# 🎵 演唱会抢票系统 PRD（产品需求文档）

## 一、项目概述

**项目名**：Concert Ticket Flash Sale System（演唱会抢票系统）

**一句话目标**：构建一个在**高并发开票场景**下，能防止超卖、限购、防刷，并实现高效下单的抢票系统。

## 二、技术栈

| 层 | 技术 | 版本/说明 |
|----|------|-----------|
| 后端框架 | Spring Boot | 3.x，单机服务 |
| 缓存 | Redis | 7.x（Windows 本机安装） |
| 消息队列 | RabbitMQ | 3.x（Windows 本机安装） |
| 数据库 | MySQL | 8.x（Windows 本机安装） |
| ORM | MyBatis-Plus | 3.5.x |
| 认证 | JWT | jjwt 0.12 |
| 前端 | Vue 3 + Vite | Composition API |
| UI 库 | Element Plus | 组件库 |
| 压测 | JMeter | 5.x |
| 构建 | Maven / npm | 后端 / 前端 |

## 三、系统架构（单机可跑）

```
浏览器 (Vue)
   │
   ▼
Spring Boot 后端
   ├── 登录/认证 (JWT)
   ├── 抢票接口 (Redis 预扣 + MQ 削峰)
   ├── 防刷限流
   └── 订单管理 (异步扣库存)
        ├── Redis ──── 库存预扣、倒计时、限流
        ├── RabbitMQ ── 下单削峰、超时释放
        └── MySQL ───── 演出、票档、订单
```

## 四、数据库设计（核心表）

### 1. `user`（用户表）
```sql
id, username, password(BCrypt加密), phone, id_card, created_at
```

### 2. `concert`（演出表）
```sql
id, name, venue, show_time, sale_start_time, sale_end_time, status, created_at
```

### 3. `ticket_tier`（票档表）
```sql
id, concert_id, name, price, total_stock, stock, limit_per_user, created_at
```
> `stock` 字段配合 Redis 预扣，`limit_per_user` 用于限购。

### 4. `orders`（订单表）
```sql
id, order_no, user_id, concert_id, tier_id, id_card, price,
status,        -- 0待支付 1已支付 2已取消 3超时
created_at, paid_at, expire_at   -- expire_at = 创建5分钟后
```

### 5. `user_ticket_limit`（限购/防重复）
```sql
id, user_id, concert_id, purchased_count
```

## 五、核心接口（API 契约）

### 认证
- `POST /api/auth/register` — 注册
- `POST /api/auth/login` — 登录，返回 JWT

### 演出
- `GET /api/concert/list` — 演唱会列表
- `GET /api/concert/{id}` — 详情（含票档、库存、倒计时）

### 抢票（核心高并发）
- `POST /api/order/seckill`
  - 入参：`{ concertId, tierId, idCard }`
  - 请求头：`Authorization: Bearer <JWT>`
  - 逻辑：限购校验 → Redis Lua 原子扣库存 → 发 MQ → 异步下单

### 订单
- `GET /api/order/list` — 我的订单
- `POST /api/order/{orderNo}/pay` — 支付
- 超时释放：MQ 延时队列，5 分钟未支付回补库存

## 六、非功能性需求（关键指标）

| 指标 | 目标 |
|------|------|
| 单机并发 | ≥ 2000 QPS（JMeter 实测） |
| 下单延迟 | P99 < 200ms |
| 防超卖 | 0 超卖 |
| 限购 | 一人一票档限 1 张 |
| 订单超时 | 5 分钟未付，自动释放库存 |
| 防刷 | 同账号/设备/IP 限频，非法请求拦截 |

## 七、验收标准

1. 后端可在本地一条命令启动（本方案：Windows 本机安装 MySQL/Redis/RabbitMQ，`mvn spring-boot:run`）
2. 前端页面能真实操作：登录 → 选场次 → 点抢票 → 支付
3. 压测 500 并发，0 超卖，无死锁，并记录优化前后 QPS
4. 提供优化前后对比数据（直接查 DB vs Redis+MQ）

## 八、项目结构

```
演唱会抢票秒杀系统/
├── PRD.md                    # 本文件
├── README.md                 # 环境搭建与启动指南
├── backend/                  # Spring Boot
└── frontend/                 # Vue 3
```

## 九、开发里程碑

| 阶段 | 内容 |
|------|------|
| M1 | Spring Boot 骨架 + 用户注册登录（JWT） |
| M2 | 演出/票档数据表 + 管理接口 + 列表/详情页 |
| M3 | 核心抢票接口（Redis Lua + MQ + 异步下单） |
| M4 | 限购、防刷限流、订单超时释放（延时队列） |
| M5 | 前端抢购页 + 支付 + 数据大屏 + 压测脚本 |
