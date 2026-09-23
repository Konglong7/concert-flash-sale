# 🎵 演唱会抢票秒杀系统

基于 **Spring Boot 3 + MyBatis-Plus + Redis + RabbitMQ + MySQL + Vue 3** 的高并发抢票系统。
需求规格见 [PRD.md](./PRD.md)。

## 一、环境要求（Windows 本机安装，不使用 Docker）

| 组件 | 版本 | 本机状态 |
|------|------|----------|
| JDK | 17+ | ✅ 已安装 |
| Maven | 3.9+ | ✅ 已安装 |
| Node.js | 18+ / npm 10+ | ✅ 已安装 |
| MySQL | 8.0 | ✅ 运行中（服务 MySQL80，root/123456，库 flash_sale 已初始化） |
| Redis | Windows 版 | ✅ 运行中（E:\Redis，密码 123456，需手动启动 redis-server.exe，未注册服务） |
| RabbitMQ | 3.13.7 | ✅ 运行中（Windows 服务自启，Erlang 位于 E:\Erlang，本体位于 E:\RabbitMQ） |

## 二、中间件初始化

### 1. MySQL
```sql
-- 命令行执行（默认账号 root，密码在 application.yml 中配置，默认 root）
mysql -uroot -p < backend/sql/schema.sql
```

### 2. Redis
- Memurai 安装后自动作为 Windows 服务运行（端口 6379）
- tporadowski 版本解压后运行 `redis-server.exe`
- 验证：`redis-cli ping` 返回 `PONG`

### 3. RabbitMQ
- 安装后服务自动启动（AMQP 端口 5672，管理台 15672）
- 启用管理台：`"C:\Program Files\RabbitMQ Server\rabbitmq_server-*\sbin\rabbitmq-plugins.bat" enable rabbitmq_management`
- 验证：浏览器打开 http://localhost:15672 （guest/guest）

## 三、后端启动

```bash
cd backend
mvn spring-boot:run
```

默认连接 `localhost` 的 MySQL(root/root)、Redis(6379)、RabbitMQ(guest/guest)。
如需覆盖，用环境变量：`MYSQL_HOST` `MYSQL_PORT` `MYSQL_USER` `MYSQL_PASSWORD` `REDIS_HOST` `REDIS_PORT` `RABBITMQ_HOST` `RABBITMQ_USER` `RABBITMQ_PASS` `JWT_SECRET`。

## 四、接口清单

统一返回体：`{"code":200, "message":"success", "data":{...}}`，业务错误 code=400/401/404/429/500。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 `{username, password, phone?, idCard?}` | ❌ |
| POST | `/api/auth/login` | 登录，返回 JWT | ❌ |
| GET | `/api/concert/list` | 演出列表（实时状态 + 倒计时秒数） | ❌ |
| GET | `/api/concert/{id}` | 演出详情（含票档、库存、限购、倒计时） | ❌ |
| POST | `/api/order/seckill` | 抢票 `{concertId, tierId, idCard}`，Redis Lua 扣减 + MQ 异步下单 | ✅ |
| GET | `/api/order/seckill/result/{tierId}` | 轮询抢票结果（processing/success/fail/none） | ✅ |
| GET | `/api/order/list` | 我的订单 | ✅ |
| POST | `/api/order/{orderNo}/pay` | 支付订单 | ✅ |
| POST | `/api/order/direct` | 【压测对照】传统直扣 DB 方案，仅用于性能对比 | ✅ |

抢票核心链路：限流(5次/10秒) → Redis 去重 → Lua 原子扣库存 → MQ → 异步落库（DB `WHERE stock>0` 双保险）→ 支付有效期 5 分钟，超时经 RabbitMQ 延时队列自动释放库存。

## 五、前端启动

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173（已配置代理到 8080）
```

页面：登录/注册 → 演出列表（状态与倒计时实时刷新）→ 抢票详情（选票档、排队弹窗）→ 我的订单（支付）。

## 六、测试

```powershell
# 功能测试（19 项断言；建议先以 PAY_EXPIRE_MINUTES=0.05 启动后端观察超时释放）
$env:PAY_EXPIRE_MINUTES='0.05'; cd backend; mvn spring-boot:run
# 另开终端
.\scripts\functional-test.ps1

# 并发压测 + 0超卖验证（100用户抢20张票）
.\scripts\load-test.ps1 -Users 100 -TierId 1 -Stock 20

# 优化前后对比（直扣DB vs Redis+MQ）
.\scripts\load-test.ps1 -Users 200 -TierId 1 -Stock 50 -Direct   # 方案A
.\scripts\load-test.ps1 -Users 200 -TierId 1 -Stock 50           # 方案B

# JMeter 500 并发压测
.\scripts\gen-users.ps1 -Count 500
jmeter -n -t scripts\seckill-test.jmx -Jcsv=scripts\users.csv -Jthreads=500 -l report.jtl
```

详见 [docs/压测报告.md](./docs/压测报告.md)。

## 七、里程碑进度

- [x] **M1** Spring Boot 骨架 + 用户注册登录（JWT + BCrypt + JWT 拦截器）
- [x] **M2** 演出/票档列表/详情接口（实时状态计算 + 开售/停售倒计时）
- [x] **M3** 核心抢票（Redis Lua 原子扣库存 + RabbitMQ 削峰 + 异步下单 + 结果轮询）
- [x] **M4** 限购、防刷限流、订单支付、订单超时释放（RabbitMQ TTL+死信延时队列）
- [x] **M5** Vue3 前端（登录/列表/抢票/订单支付）+ 压测脚本 + 压测报告
