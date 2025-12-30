# 报告章节TODO清单

本文档按照报告结构，为每个章节部分提供详细的写作指导说明，便于后续拆分和撰写。

---

## 第二章 概要设计

### 2.1. 系统概述

**需要撰写的内容：**
- 系统定位和目标：C语言在线代码编辑、编译和运行环境，面向学习者和开发者
- 系统功能概述：代码编辑、编译执行、结果展示、用户认证、历史记录管理等核心功能
- 技术架构概述：前后端分离架构，Spring Boot后端 + Vue.js前端
- 系统特点：实时编译执行、Docker沙箱隔离、JWT认证、WebSocket实时推送
- 应用场景：C语言学习、代码测试、算法验证、教学演示等

**提示词关键词：** 系统定位、功能概述、技术架构、系统特点、应用场景

---

### 2.2. 总体结构和功能模块设计

#### 2.2.1. 系统总体结构

**需要撰写的内容：**
- 系统架构图：展示前后端分离架构、各层职责划分
- 分层架构说明：
  - **接口层（Interfaces Layer）**：RESTful API控制器、WebSocket处理器
  - **应用层（Application Layer）**：认证服务、代码执行服务、代码片段服务
  - **领域层（Domain Layer）**：用户实体、代码片段实体、执行记录实体
  - **基础设施层（Infrastructure Layer）**：数据访问、缓存、Docker容器池、安全组件
- 技术栈说明：Java 17 + Spring Boot 3.x、Vue.js 3、MySQL/TiDB、Redis、Docker
- 部署架构：前端静态资源、后端服务、数据库、缓存、Docker沙箱环境

**提示词关键词：** 系统架构图、分层架构、技术栈、部署架构、前后端分离

---

#### 2.2.2. 功能模块划分

**需要撰写的内容：**
- **认证授权模块（Auth Module）**
  - 功能：用户注册、登录、JWT令牌生成与校验、权限控制
  - 技术实现：Spring Security + JWT、BCrypt密码加密、Redis令牌管理
  - 接口：POST /api/auth/register、POST /api/auth/login、GET /api/user
  
- **代码处理模块（Code Module）**
  - 功能：代码保存、编译执行、结果推送、代码片段管理
  - 技术实现：Docker容器池、异步执行、超时控制、输出限流
  - 接口：POST /api/code/run、POST /api/code/snippets、WebSocket实时推送
  
- **执行记录模块（Result Module）**
  - 功能：执行历史查询、记录统计、结果展示
  - 技术实现：JPA Repository、分页查询、时间索引优化
  - 接口：GET /api/code/history、GET /api/code/records
  
- **基础设施模块（Infrastructure Module）**
  - 功能：数据持久化、缓存管理、容器池管理、安全过滤、异常处理
  - 技术实现：Spring Data JPA、Redis、Docker API、全局异常处理器

**提示词关键词：** 功能模块、认证授权、代码处理、执行记录、基础设施、模块职责

---

### 2.3. 模块结构及关系

#### 2.3.1. 模块调用关系

**需要撰写的内容：**
- 模块调用关系图：展示各模块之间的依赖和调用关系
- 调用流程说明：
  - **用户认证流程**：前端 → AuthController → AuthService → UserRepository → 数据库
  - **代码执行流程**：前端 → CodeController → CodeExecutionService → Docker容器池 → 结果推送
  - **代码保存流程**：前端 → CodeSnippetController → CodeSnippetService → CodeSnippetRepository → 数据库
- 依赖关系：应用层依赖领域层、基础设施层，接口层依赖应用层
- 服务间通信：同步HTTP调用、异步WebSocket推送、Redis缓存共享

**提示词关键词：** 模块调用关系图、调用流程、依赖关系、服务通信、数据流向

---

#### 2.3.2. 数据流图

**需要撰写的内容：**
- 数据流图（DFD）：展示数据在系统中的流动过程
- 主要数据流：
  - **用户数据流**：注册/登录 → 验证 → 存储/查询 → 返回JWT令牌
  - **代码数据流**：代码输入 → 验证 → 保存/执行 → 结果返回
  - **执行结果流**：编译执行 → 结果收集 → 实时推送/持久化 → 前端展示
- 数据存储：数据库（用户、代码片段、执行记录）、Redis缓存（令牌、临时数据）
- 外部实体：用户、Docker容器、编译器、文件系统

**提示词关键词：** 数据流图、数据流向、数据存储、外部实体、数据加工、数据流层次

---

### 2.4. 接口设计

#### 2.4.1. 外部接口

**需要撰写的内容：**
- RESTful API接口规范：
  - **认证接口**：/api/auth/register、/api/auth/login、/api/auth/logout
  - **代码执行接口**：POST /api/code/run（请求参数：code、title；响应：执行结果）
  - **代码片段接口**：GET /api/code/snippets、POST /api/code/snippets、PUT /api/code/snippets/{id}
  - **历史记录接口**：GET /api/code/history、GET /api/code/records/{id}
- WebSocket接口：
  - **连接地址**：/ws/execution-result
  - **消息格式**：JSON格式，包含执行状态、输出、错误信息
- 接口认证：JWT Bearer Token认证机制
- 接口规范：统一响应格式（code、message、data）、错误码定义、请求/响应示例

**提示词关键词：** RESTful API、WebSocket接口、接口规范、认证机制、请求响应格式、错误处理

---

#### 2.4.2. 内部接口

**需要撰写的内容：**
- 服务层接口：
  - **AuthService接口**：register()、login()、validateToken()、getCurrentUser()
  - **CodeExecutionService接口**：compileAndRun()、executeAsync()、getExecutionResult()
  - **CodeSnippetService接口**：createSnippet()、updateSnippet()、deleteSnippet()、getSnippets()
- Repository接口：
  - **UserRepository**：findByUsername()、findByEmail()、existsByUsername()
  - **CodeSnippetRepository**：findByUserId()、findByUserIdAndTitle()
  - **ExecutionRecordRepository**：findByUserIdOrderByCreatedAtDesc()
- 基础设施接口：
  - **ContainerPool接口**：acquireContainer()、releaseContainer()、getStatus()
  - **JwtTokenUtils接口**：generateToken()、validateToken()、getUsernameFromToken()
- 接口参数和返回值说明：方法签名、参数类型、返回值类型、异常处理

**提示词关键词：** 服务接口、Repository接口、基础设施接口、方法签名、参数说明、返回值说明

---

### 2.5. 数据结构和数据库设计

#### 2.5.1. 数据库概念结构设计 (E-R图)

**需要撰写的内容：**
- E-R图绘制：展示实体、属性、关系
- 实体说明：
  - **用户实体（User）**：id、username、password_hash、email、role、enabled、created_at
  - **代码片段实体（CodeSnippet）**：id、user_id、title、code_content、language、is_public、created_at、updated_at
  - **执行记录实体（ExecutionRecord）**：id、user_id、title、code、output、error、exit_code、created_at
- 关系说明：
  - **User → CodeSnippet**：一对多关系，一个用户拥有多个代码片段
  - **User → ExecutionRecord**：一对多关系，一个用户有多条执行记录
- 实体属性详细说明：字段含义、数据类型、约束条件

**提示词关键词：** E-R图、实体、属性、关系、一对多、实体关系模型、概念模型

---

#### 2.5.2. 数据库逻辑结构设计 (表结构)

**需要撰写的内容：**
- 表结构详细设计：
  - **user表**：字段名、数据类型、长度、约束、默认值、说明、索引设计
  - **code_snippet表**：字段定义、外键约束、索引优化
  - **execution_record表**：字段定义、外键约束、索引优化
- 索引设计说明：
  - 主键索引、唯一索引、普通索引、复合索引
  - 索引优化策略：查询优化、性能提升
- 约束设计：
  - 主键约束、外键约束、唯一约束、非空约束、默认值约束
  - 级联删除策略：用户删除时级联删除相关记录
- 表结构SQL语句：完整的CREATE TABLE语句

**提示词关键词：** 表结构、字段定义、数据类型、索引设计、约束设计、SQL语句、性能优化

---

## 第三章 详细设计

### 3.1. 全局变量和数据结构设计

**需要撰写的内容：**
- 全局配置变量：
  - **应用配置**：服务器端口、数据库连接、Redis连接、JWT密钥、过期时间
  - **代码执行配置**：Docker镜像名称、容器超时时间、代码长度限制、输出大小限制
  - **容器池配置**：池大小、最大空闲时间、容器回收策略
- 全局常量定义：
  - **响应码常量**：ApiResponseCode枚举（SUCCESS、UNAUTHORIZED、BAD_REQUEST等）
  - **用户角色常量**：ROLE_USER、ROLE_ANONYMOUS
  - **执行状态常量**：SUCCESS、FAILED、TIMEOUT
- 数据结构设计：
  - **请求DTO**：RegisterRequest、LoginRequest、CodeRunRequest、SnippetCreateRequest
  - **响应DTO**：ApiResponse、ExecutionResult、SnippetResponse、RecordResponse
  - **内部数据结构**：ContainerInfo、PoolStatus、CompilationResult
- 枚举类型：用户角色、执行状态、响应码、语言类型

**提示词关键词：** 全局变量、配置变量、常量定义、数据结构、DTO、枚举类型、数据模型

---

### 3.2. 功能模块详细设计

#### 3.2.1. xxx 模块

**说明：** 此处需要为每个功能模块分别撰写详细设计，包括：
- 认证授权模块（Auth Module）
- 代码处理模块（Code Module）
- 执行记录模块（Result Module）
- 基础设施模块（Infrastructure Module）

**每个模块需要撰写的内容：**
- **模块概述**：模块功能、职责、在系统中的作用
- **类设计**：主要类及其职责、类之间的关系
- **方法设计**：核心方法的详细设计
  - 方法签名：方法名、参数列表、返回值类型
  - 方法功能：方法的作用、处理逻辑
  - 算法流程：详细的处理步骤、流程图
  - 异常处理：可能抛出的异常、异常处理策略
- **关键算法**：核心业务逻辑的算法实现（如JWT生成、密码加密、代码编译执行流程）
- **状态管理**：模块内部状态、状态转换、状态持久化
- **性能优化**：缓存策略、异步处理、资源池管理

**提示词关键词：** 模块设计、类设计、方法设计、算法流程、异常处理、状态管理、性能优化

---

### 3.3. 模块间接口详细设计

#### 3.3.1. 接口参数说明

**需要撰写的内容：**
- **RESTful API接口参数**：
  - 请求参数：参数名、类型、是否必填、默认值、说明、示例值
  - 路径参数：参数说明、格式要求
  - 查询参数：分页参数、过滤参数、排序参数
  - 请求体：JSON格式、字段说明
  - 响应参数：响应结构、字段说明、状态码含义
- **WebSocket接口参数**：
  - 连接参数：认证方式、连接URL
  - 消息格式：发送消息格式、接收消息格式、消息类型
- **服务层接口参数**：
  - 方法参数：参数类型、参数说明、参数验证规则
  - 返回值：返回类型、返回值说明、可能的值
- **接口示例**：完整的请求/响应示例、错误响应示例

**提示词关键词：** 接口参数、请求参数、响应参数、参数说明、参数验证、接口示例、API文档

---

#### 3.3.2. 调用时序图

**需要撰写的内容：**
- **用户注册时序图**：
  - 参与者：前端、AuthController、AuthService、UserRepository、数据库
  - 消息流：请求 → 验证 → 密码加密 → 保存 → 返回结果
- **用户登录时序图**：
  - 参与者：前端、AuthController、AuthService、UserRepository、JwtTokenUtils、Redis
  - 消息流：登录请求 → 验证密码 → 生成JWT → 存储令牌 → 返回令牌
- **代码执行时序图**：
  - 参与者：前端、CodeController、CodeExecutionService、ContainerPool、Docker、WebSocket
  - 消息流：代码提交 → 获取容器 → 编译执行 → 实时推送结果 → 释放容器
- **代码保存时序图**：
  - 参与者：前端、CodeSnippetController、CodeSnippetService、CodeSnippetRepository、数据库
  - 消息流：保存请求 → 验证 → 持久化 → 返回结果
- 时序图说明：每个步骤的详细说明、异常处理流程、超时处理

**提示词关键词：** 时序图、调用流程、参与者、消息流、交互流程、UML时序图、流程说明

---

### 如何基于此TODO清单撰写报告

1. **按章节顺序撰写**：从2.1开始，逐章节完成内容撰写
2. **参考提示词关键词**：每个部分都提供了关键词，可以作为详细描述的提示词使用
3. **结合代码实现**：撰写时参考实际代码实现，确保设计与实现一致
4. **补充图表**：需要绘制架构图、E-R图、时序图、数据流图等
5. **完善细节**：每个模块需要详细说明设计思路、实现方式、技术选型理由

### 后续拆分建议

- 可以将每个章节拆分为独立的文档文件
- 每个模块的详细设计可以单独撰写
- 接口文档可以单独维护
- 数据库设计可以单独成章

---


