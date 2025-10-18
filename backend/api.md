## API 接口清单

### 1. 认证相关接口 ([AuthController.java](file://D:\代码集合\codelab\C-CodeLab\backend\src\main\java\com\codelab\interfaces\web\AuthController.java))

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/user` | 获取当前用户 |

### 2. 用户资料相关接口 ([UserProfileController.java](file://D:\代码集合\codelab\C-CodeLab\backend\src\main\java\com\codelab\interfaces\web\UserProfileController.java))

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/user/profile` | 获取用户资料 |
| PUT | `/api/user/profile` | 更新用户资料 |
| PUT | `/api/user/password` | 修改密码 |

### 3. 代码操作相关接口 ([CodeController.java](file://D:\代码集合\codelab\C-CodeLab\backend\src\main\java\com\codelab\interfaces\web\CodeController.java))

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/code/save` | 保存代码片段 |
| POST | `/api/code/run` | 运行代码 |

### 4. 用户代码管理接口 ([UserCodeController.java](file://D:\代码集合\codelab\C-CodeLab\backend\src\main\java\com\codelab\interfaces\web\UserCodeController.java))

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/user/code-snippets` | 获取用户代码片段列表 |
| GET | `/api/user/execution-records` | 获取用户执行记录 |
| DELETE | `/api/user/code-snippets/{id}` | 删除指定代码片段 |
| DELETE | `/api/user/execution-records/{id}` | 删除指定执行记录 |

### 5. 结果查询接口 ([ResultController.java](file://D:\代码集合\codelab\C-CodeLab\backend\src\main\java\com\codelab\interfaces\web\ResultController.java))

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/result/list` | 获取最近的执行结果列表 |

## 在APIFox中设置API的步骤

1. **创建项目**：
    - 登录APIFox，点击"新建项目"
    - 输入项目名称（如"C-CodeLab Backend"）
    - 选择合适的分类并创建项目

2. **设置环境变量**：
    - 点击"环境管理"
    - 添加环境变量，例如：
        - `{{base_url}}`: http://localhost:8080 （本地开发环境）
        - `{{api_prefix}}`: /api

3. **创建目录结构**：
   按照功能模块创建如下目录：
    - 认证模块
    - 用户模块
    - 代码模块
    - 结果模块

4. **添加API接口**：
   对于每个API，在对应的目录下创建接口文档：
    - 设置请求方法（GET/POST/PUT/DELETE）
    - 设置路径（使用环境变量，如 `{{base_url}}{{api_prefix}}/auth/login`）
    - 设置请求头（Content-Type: application/json）
    - 设置请求参数/请求体
    - 设置响应示例

5. **配置认证方式**：
    - 由于系统使用Session进行认证，你需要在APIFox中启用Cookie管理
    - 在"项目设置"->"全局参数设置"中开启"自动重定向"和"携带Cookie"

## 各接口详细说明

### 认证相关接口

1. **用户注册** (`/api/auth/register`)
    - 请求方式：POST
    - 请求体：
      ```json
      {
        "username": "用户名",
        "password": "密码（需包含字母、数字和特殊字符，至少8位）",
        "email": "邮箱地址"
      }
      ```


2. **用户登录** (`/api/auth/login`)
    - 请求方式：POST
    - 请求体：
      ```json
      {
        "username": "用户名",
        "password": "密码"
      }
      ```


### 用户资料相关接口

3. **获取用户资料** (`/api/user/profile`)
    - 请求方式：GET
    - 需要登录状态

4. **更新用户资料** (`/api/user/profile`)
    - 请求方式：PUT
    - 请求体：
      ```json
      {
        "email": "新的邮箱地址"
      }
      ```

    - 需要登录状态

5. **修改密码** (`/api/user/password`)
    - 请求方式：PUT
    - 请求体：
      ```json
      {
        "oldPassword": "原密码",
        "newPassword": "新密码（需包含字母、数字和特殊字符，至少8位）"
      }
      ```

    - 需要登录状态

### 代码操作相关接口

6. **保存代码片段** (`/api/code/save`)
    - 请求方式：POST
    - 请求体：
      ```json
      {
        "title": "代码标题",
        "codeContent": "代码内容",
        "language": "编程语言（如c）",
        "isPublic": false
      }
      ```

    - 需要登录状态

7. **运行代码** (`/api/code/run`)
    - 请求方式：POST
    - 请求体：
      ```json
      {
        "code": "要执行的代码",
        "title": "代码标题（可选）"
      }
      ```

    - 需要登录状态

### 用户代码管理接口

8. **获取代码片段列表** (`/api/user/code-snippets`)
    - 请求方式：GET
    - 查询参数：
        - page: 页码（默认0）
        - size: 每页大小（默认10）
    - 需要登录状态

9. **获取执行记录** (`/api/user/execution-records`)
    - 请求方式：GET
    - 查询参数：
        - page: 页码（默认0）
        - size: 每页大小（默认10）
    - 需要登录状态

10. **删除代码片段** (`/api/user/code-snippets/{id}`)
    - 请求方式：DELETE
    - 路径参数：id（代码片段ID）
    - 需要登录状态

11. **删除执行记录** (`/api/user/execution-records/{id}`)
    - 请求方式：DELETE
    - 路径参数：id（执行记录ID）
    - 需要登录状态

### 结果查询接口

12. **获取执行结果列表** (`/api/result/list`)
    - 请求方式：GET
    - 需要登录状态

## 测试建议

1. 先测试认证接口，获取有效的会话
2. 使用获取到的会话测试其他需要认证的接口
3. 注意各接口的参数验证规则，特别是密码复杂度要求
4. 对于分页接口，可以调整page和size参数测试不同情况

这些就是在APIFox中设置该项目API的方法。你可以按照这个结构来组织你的API文档和测试工作。