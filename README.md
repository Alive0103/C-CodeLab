# C-CodeLab - 在线C语言编程学习平台

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.0-4FC08D.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📖 项目简介

C-CodeLab 是一个基于 Web 的在线 C 语言编程学习平台，提供实时代码编译、执行和结果展示功能。学生可以在浏览器中编写 C 代码，实时查看编译和执行结果，支持代码保存和历史记录查询。

### ✨ 核心功能

- 🔐 **JWT 认证系统** - 安全的用户认证和授权
- 💻 **在线代码编辑器** - 支持语法高亮的代码编辑
- ⚡ **实时代码执行** - 支持 C 语言代码的在线编译和执行
- 💾 **代码保存管理** - 支持代码片段的保存和分类
- 📊 **执行历史记录** - 查看历史执行记录和结果
- 🔄 **WebSocket 推送** - 实时推送执行结果
- 👤 **用户个人中心** - 个人信息管理和设置

## 🏗️ 技术架构

### 后端技术栈
- **Java 17** - 编程语言
- **Spring Boot 3.3.3** - 应用框架
- **Spring Security 6** - 安全框架
- **Spring Data JPA** - 数据访问层
- **JWT (jjwt)** - 无状态认证
- **Redis** - 缓存和会话管理
- **MySQL/TiDB** - 数据存储
- **WebSocket** - 实时通信
- **Maven** - 依赖管理

### 前端技术栈
- **Vue 3** - 前端框架
- **TypeScript** - 类型安全
- **Pinia** - 状态管理
- **Axios** - HTTP 客户端
- **Vite** - 构建工具
- **Element Plus** - UI 组件库

## 🚀 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.6+
- MySQL 8.0+ 或 TiDB
- Redis 6.0+

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-username/C-CodeLab.git
cd C-CodeLab
```

2. **配置数据库**
```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/codelab?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
```

3. **启动后端服务**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/c-codelab-0.0.1-SNAPSHOT.jar
```

4. **启动前端服务**
```bash
cd front
npm install
npm run dev
```

5. **访问应用**
- 前端地址：http://localhost:3000
- 后端API：http://localhost:8081

## 📚 API 文档

### 认证接口

| 接口 | 方法 | 描述 | 请求体 |
|------|------|------|--------|
| `/api/auth/register` | POST | 用户注册 | `{username, password, confirmPassword, email}` |
| `/api/auth/login` | POST | 用户登录 | `{username, password}` |
| `/api/auth/logout` | POST | 用户登出 | 需要 Authorization 头 |
| `/api/user` | GET | 获取用户信息 | 需要 Authorization 头 |

### 代码接口

| 接口 | 方法 | 描述 | 请求体 |
|------|------|------|--------|
| `/api/code/run` | POST | 执行代码 | `{code, title?}` |
| `/api/code/save` | POST | 保存代码 | `{title, codeContent, language, isPublic}` |
| `/api/code/list` | GET | 获取代码列表 | 需要 Authorization 头 |

### 历史记录接口

| 接口 | 方法 | 描述 | 请求体 |
|------|------|------|--------|
| `/api/result/list` | GET | 获取执行历史 | 需要 Authorization 头 |

### WebSocket 接口

| 端点 | 描述 | 认证 |
|------|------|------|
| `/ws/execution-result` | 执行结果推送 | 需要 JWT Token |

## 🔧 配置说明

### 数据库配置

项目支持 MySQL 和 TiDB 数据库：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/your-database
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: your-redis-host
      port: 6379
      password: your_redis_password
      ssl:
        enabled: true  # 如果使用 SSL
```

### JWT 配置

```yaml
jwt:
  secret: your-secret-key  # 生产环境请使用强密钥
  expiration: 86400000    # 24小时（毫秒）
```

## 🏃‍♂️ 运行指南

### 开发环境

1. **启动后端**
```bash
cd backend
mvn spring-boot:run
```

2. **启动前端**
```bash
cd front
npm run dev
```

### 生产环境

1. **构建后端**
```bash
cd backend
mvn clean package -DskipTests
```

2. **构建前端**
```bash
cd front
npm run build
```

3. **部署**
```bash
# 后端
java -jar target/c-codelab-0.0.1-SNAPSHOT.jar

# 前端（使用 Nginx 或其他 Web 服务器）
# 将 dist 目录部署到 Web 服务器
```

## 📁 项目结构

```
C-CodeLab/
├── backend/                 # 后端项目
│   ├── src/main/java/
│   │   └── com/codelab/
│   │       ├── application/     # 应用服务层
│   │       ├── domain/          # 领域模型
│   │       ├── infrastructure/  # 基础设施层
│   │       ├── interfaces/      # 接口层
│   │       └── service/         # 业务服务
│   ├── src/main/resources/
│   │   ├── application.yml      # 配置文件
│   │   └── sql/                 # SQL 脚本
│   └── pom.xml                  # Maven 配置
├── front/                   # 前端项目
│   ├── src/
│   │   ├── api/             # API 接口
│   │   ├── components/       # Vue 组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── utils/            # 工具函数
│   │   └── views/            # 页面视图
│   ├── package.json         # 依赖配置
│   └── vite.config.ts       # Vite 配置
├── docs/                    # 文档目录
│   ├── 后端详细设计.md
│   ├── 前端详细设计.md
│   └── JWT认证系统实现说明.md
└── README.md                # 项目说明
```

## 🔒 安全特性

### JWT 认证
- **无状态认证** - 支持水平扩展
- **令牌管理** - Redis 存储有效令牌
- **自动过期** - 24小时令牌有效期
- **安全签名** - HS512 算法签名

### 密码安全
- **BCrypt 哈希** - 强度12的密码加密
- **随机盐值** - 每个用户独立盐值
- **密码策略** - 强密码要求

### 代码执行安全
- **沙箱隔离** - 安全的代码执行环境
- **资源限制** - CPU、内存、时间限制
- **输入验证** - 严格的输入参数验证

## 🧪 测试

### 后端测试
```bash
cd backend
mvn test
```

### 前端测试
```bash
cd front
npm run test
```

### API 测试示例

**用户注册**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!",
    "confirmPassword": "TestPass123!",
    "email": "test@example.com"
  }'
```

**用户登录**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

**代码执行**
```bash
curl -X POST http://localhost:8081/api/code/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "code": "#include <stdio.h>\nint main(){ printf(\"Hello, World!\\n\"); return 0; }",
    "title": "Hello World"
  }'
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👥 作者

- **开发者** - [Alive0103](https://github.com/Alive0103)
- **项目链接** - [https://github.com/Alive0103/XDU-CS-labb](https://github.com/Alive0103/XDU-CS-lab)

## 🙏 致谢

- Spring Boot 团队提供的优秀框架
- Vue.js 团队提供的前端框架
- 所有开源贡献者的支持

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 📧 Email: wangyueyang_xd26@163.com
- 🐛 Issues: [GitHub Issues](https://github.com/Alive0103/C-CodeLab/issues)
- 💬 Discussions

---

⭐ 如果这个项目对您有帮助，请给它一个星标！
