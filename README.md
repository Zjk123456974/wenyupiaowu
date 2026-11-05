# 🎫 智能文娱票务预约抢票系统

基于 **Spring Boot + Vue 3** 的全栈票务平台，面向演出、展览等文娱场景，支持场次浏览、在线选座、下单支付、二维码核销等完整业务流程。

> 毕业设计项目 · 前后端分离架构

---

## ✨ 功能特性

- 🔐 **用户体系**：注册 / 登录 / JWT 鉴权 / 角色权限
- 🎭 **演出管理**：场次、票档、库存的增删改查
- 🪑 **在线选座**：可视化座位图，实时库存锁定
- ⚡ **抢票削峰**：Redis 缓存 + 限流，应对高并发秒杀场景
- 📱 **二维码核销**：HMAC 签名二维码，支持现场扫码验票
- 📊 **数据看板**：ECharts 可视化运营数据

---

## 🛠 技术栈

### 后端 (`ticket-system`)
| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 3.x |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7.x |
| 安全 | JWT（自定义拦截器） |
| 模板 | Thymeleaf |

### 前端 (`ticket-system-frontend`)
| 分类 | 技术 |
|---|---|
| 框架 | Vue 3 + Vite 5 |
| UI | Element Plus |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| 请求 | Axios |
| 图表 | ECharts |
| 扫码 | html5-qrcode / qrcode |

---

## 📂 目录结构

```
.
├── ticket-system/              # 后端（Spring Boot）
│   ├── src/                    # 源码
│   ├── sql/                    # 数据库脚本
│   ├── pom.xml
│   └── src/main/resources/
│       └── application.example.yml   # 配置模板（不含真实密码）
├── ticket-system-frontend/     # 前端（Vue 3）
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── vite.config.js
├── docs/diagrams/              # 架构图 / 用例图（PlantUML）
└── README.md
```

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.x
- Redis 7.x

### 后端
```bash
cd ticket-system
# 1. 复制配置模板并填入本地数据库密码
cp src/main/resources/application.example.yml src/main/resources/application.yml
# 2. 导入 sql/ 下的数据库脚本
# 3. 启动
mvn spring-boot:run        # 默认端口 9090
```

### 前端
```bash
cd ticket-system-frontend
npm install
npm run dev                # 默认 http://localhost:5173
```

---

## ⚠️ 配置说明

仓库**不包含**真实 `application.yml`（含数据库密码与 JWT 密钥）。
请依据 `application.example.yml` 在本地创建自己的 `application.yml`，
并将 `jwt.secret` 与 `ticket.qr.secret` 替换为强随机值后再部署到公网。

---

## 📄 许可证

本项目仅供学习与毕设演示使用。
