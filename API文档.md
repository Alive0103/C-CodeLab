# C-CodeLab API 文档

## 📋 概述

本文档描述了 C-CodeLab 项目的 RESTful API 接口规范。

## 🔐 认证方式

项目使用 Session 认证方式，用户登录后服务器会创建会话，后续请求会自动携带会话信息。

## 📡 基础信息

- **Base URL**: `http://localhost:8081/api`
- **Content-Type**: `application/json`
- **认证方式**: Session Cookie

## 🚀 API 接口

### 认证相关

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**响应**:
```json
{
  "success": true,
  "data": "注册成功",
  "message": null
}
```

**验证规则**:
- `username`: 必填，字符串
- `password`: 必填，至少8位，包含字母、数字和特殊字符(@$%!%*#?&)
- `email`: 必填，有效邮箱格式

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "user123",
    "email": "user@example.com",
    "role": "ROLE_USER",
    "enabled": true,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "message": null
}
```

#### 用户登出
```http
POST /api/auth/logout
```

**响应**:
```json
{
  "success": true,
  "data": "登出成功",
  "message": null
}
```

#### 获取当前用户信息
```http
GET /api/user
```

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "user123",
    "email": "user@example.com",
    "role": "ROLE_USER",
    "enabled": true,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "message": null
}
```

### 用户信息管理

#### 获取用户详细信息
```http
GET /api/user/profile
```

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "user123",
    "email": "user@example.com",
    "role": "ROLE_USER",
    "enabled": true,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "message": null
}
```

#### 更新用户信息
```http
PUT /api/user/profile
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

**响应**:
```json
{
  "success": true,
  "data": "更新成功",
  "message": null
}
```

#### 修改密码
```http
PUT /api/user/password
Content-Type: application/json

{
  "oldPassword": "string",
  "newPassword": "string"
}
```

**响应**:
```json
{
  "success": true,
  "data": "密码修改成功",
  "message": null
}
```

### 代码历史管理

#### 获取代码片段列表
```http
GET /api/user/code-snippets?page=0&size=10
```

**参数**:
- `page`: 页码，默认0
- `size`: 每页大小，默认10

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Hello World",
      "codeContent": "#include <stdio.h>\nint main() { printf(\"Hello, World!\\n\"); return 0; }",
      "language": "c",
      "isPublic": false,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "message": null
}
```

#### 获取执行记录列表
```http
GET /api/user/execution-records?page=0&size=10
```

**响应**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Hello World",
        "code": "#include <stdio.h>\nint main() { printf(\"Hello, World!\\n\"); return 0; }",
        "output": "Hello, World!\n",
        "error": null,
        "exitCode": 0,
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  },
  "message": null
}
```

#### 删除代码片段
```http
DELETE /api/user/code-snippets/{id}
```

**响应**:
```json
{
  "success": true,
  "data": "删除成功",
  "message": null
}
```

#### 删除执行记录
```http
DELETE /api/user/execution-records/{id}
```

**响应**:
```json
{
  "success": true,
  "data": "删除成功",
  "message": null
}
```

### 代码执行

#### 保存代码片段
```http
POST /api/code/save
Content-Type: application/json

{
  "title": "string",
  "codeContent": "string",
  "language": "c",
  "isPublic": false
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Hello World",
    "codeContent": "#include <stdio.h>\nint main() { printf(\"Hello, World!\\n\"); return 0; }",
    "language": "c",
    "isPublic": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "message": null
}
```

#### 执行代码
```http
POST /api/code/run
Content-Type: application/json

{
  "code": "string",
  "title": "string"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "success": true,
    "output": "Hello, World!\n",
    "error": "",
    "exitCode": 0
  },
  "message": null
}
```

#### 获取执行结果
```http
GET /api/code/results
```

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Hello World",
      "exitCode": 0,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "message": null
}
```

## 📊 错误码说明

| 状态码 | 说明 | 示例 |
|--------|------|------|
| 200 | 请求成功 | 正常响应 |
| 400 | 请求参数错误 | 密码格式不正确 |
| 401 | 未认证 | 未登录或会话过期 |
| 404 | 资源不存在 | 用户不存在 |
| 405 | 请求方法不支持 | GET请求登录接口 |
| 500 | 服务器内部错误 | 系统异常 |

## 🔒 安全说明

### 认证要求
- 除注册、登录接口外，所有接口都需要用户认证
- 未认证请求会返回 401 状态码
- 会话超时后需要重新登录

### 数据验证
- 所有输入数据都会进行格式验证
- 密码必须符合复杂度要求
- 邮箱格式必须正确
- 防止 SQL 注入和 XSS 攻击

### 权限控制
- 用户只能访问自己的数据
- 删除操作需要确认
- 敏感操作需要验证

## 📝 使用示例

### JavaScript (Axios)
```javascript
// 用户登录
const login = async (username, password) => {
  const response = await axios.post('/api/auth/login', {
    username,
    password
  });
  return response.data;
};

// 获取代码片段
const getCodeSnippets = async (page = 0, size = 10) => {
  const response = await axios.get(`/api/user/code-snippets?page=${page}&size=${size}`);
  return response.data;
};

// 执行代码
const runCode = async (code, title) => {
  const response = await axios.post('/api/code/run', {
    code,
    title
  });
  return response.data;
};
```

### cURL 示例
```bash
# 用户注册
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123!","email":"test@example.com"}'

# 用户登录
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123!"}'

# 获取用户信息
curl -X GET http://localhost:8081/api/user \
  -H "Cookie: JSESSIONID=your-session-id"
```

## 🔄 更新日志

### v1.0.0 (2024-01-01)
- 初始版本发布
- 基础认证功能
- 代码编辑和执行
- 用户信息管理
- 代码历史管理
