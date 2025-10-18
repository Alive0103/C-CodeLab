# GitHub 提交指南

## 🚀 准备提交到 GitHub

### 1. 初始化 Git 仓库

```bash
# 在项目根目录执行
git init
```

### 2. 添加所有文件

```bash
# 添加所有文件到暂存区
git add .

# 或者选择性添加
git add README.md
git add LICENSE
git add .gitignore
git add backend/
git add front/
git add docs/
```

### 3. 提交更改

```bash
# 首次提交
git commit -m "feat: 初始化 C-CodeLab 项目

- 实现 JWT 认证系统
- 完成用户注册、登录、登出功能
- 实现在线 C 语言代码执行
- 支持代码保存和历史记录
- 集成 WebSocket 实时推送
- 使用 Spring Boot 3.x + Vue 3 技术栈
- 支持 MySQL/TiDB 数据库
- 集成 Redis 缓存管理"
```

### 4. 创建 GitHub 仓库

1. 登录 GitHub
2. 点击 "New repository"
3. 仓库名称：`C-CodeLab`
4. 描述：`在线C语言编程学习平台 - 基于Spring Boot + Vue.js`
5. 选择 Public 或 Private
6. 不要初始化 README（已有）
7. 点击 "Create repository"

### 5. 连接远程仓库

```bash
# 添加远程仓库（替换为你的用户名）
git remote add origin https://github.com/your-username/C-CodeLab.git

# 或者使用 SSH
git remote add origin git@github.com:your-username/C-CodeLab.git
```

### 6. 推送到 GitHub

```bash
# 推送到主分支
git branch -M main
git push -u origin main
```

## 📋 提交信息规范

### 提交类型

- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 示例

```bash
# 新功能
git commit -m "feat(auth): 实现JWT认证系统

- 添加JWT令牌生成和验证
- 集成Redis存储有效令牌
- 实现用户登录状态管理
- 添加密码加密和验证"

# 修复bug
git commit -m "fix(execution): 修复代码执行超时问题

- 调整执行超时时间
- 优化内存使用
- 修复并发执行问题"

# 文档更新
git commit -m "docs: 更新API文档

- 添加认证接口说明
- 更新代码执行接口文档
- 完善错误码说明"
```

## 🏷️ 标签管理

### 创建版本标签

```bash
# 创建版本标签
git tag -a v1.0.0 -m "C-CodeLab v1.0.0 - 初始版本发布"

# 推送标签
git push origin v1.0.0
```

### 版本号规范

- `v1.0.0` - 主版本号.次版本号.修订号
- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

## 📝 分支管理

### 主分支策略

```bash
# 主分支
main          # 生产环境分支
develop       # 开发环境分支

# 功能分支
feature/auth-system     # 认证系统功能
feature/code-execution  # 代码执行功能
feature/user-profile    # 用户资料功能

# 修复分支
hotfix/security-fix     # 安全修复
hotfix/performance-fix   # 性能修复
```

### 创建功能分支

```bash
# 从develop分支创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/new-feature

# 开发完成后合并
git checkout develop
git merge feature/new-feature
git push origin develop
```

## 🔄 持续集成

### GitHub Actions 配置

创建 `.github/workflows/ci.yml`:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    - name: Run tests
      run: cd backend && mvn test

  frontend-test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: front/package-lock.json
    - name: Install dependencies
      run: cd front && npm ci
    - name: Run tests
      run: cd front && npm test
```

## 📊 项目统计

### 代码统计

```bash
# 统计代码行数
find . -name "*.java" -o -name "*.vue" -o -name "*.ts" | xargs wc -l

# 统计文件数量
find . -name "*.java" | wc -l  # Java文件
find . -name "*.vue" | wc -l   # Vue文件
find . -name "*.ts" | wc -l    # TypeScript文件
```

### 提交统计

```bash
# 查看提交统计
git log --oneline | wc -l

# 查看贡献者
git shortlog -sn

# 查看提交历史
git log --graph --oneline --all
```

## 🚀 发布流程

### 1. 准备发布

```bash
# 更新版本号
# 在 pom.xml 中更新版本
# 在 package.json 中更新版本

# 更新 CHANGELOG.md
# 添加新功能说明
# 记录bug修复
# 记录破坏性变更
```

### 2. 创建发布分支

```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0
```

### 3. 测试和修复

```bash
# 运行完整测试
mvn clean test
npm test

# 修复发现的问题
# 提交修复
git commit -m "fix: 修复发布前发现的问题"
```

### 4. 合并到主分支

```bash
git checkout main
git merge release/v1.1.0
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin main --tags
```

### 5. 清理

```bash
git branch -d release/v1.1.0
git push origin --delete release/v1.1.0
```

## 📋 检查清单

### 提交前检查

- [ ] 代码编译通过
- [ ] 所有测试通过
- [ ] 代码格式化
- [ ] 注释完整
- [ ] 文档更新
- [ ] 提交信息规范
- [ ] 敏感信息已移除

### 发布前检查

- [ ] 版本号更新
- [ ] CHANGELOG更新
- [ ] README更新
- [ ] 依赖更新
- [ ] 安全扫描
- [ ] 性能测试
- [ ] 兼容性测试

## 🔒 安全注意事项

### 敏感信息处理

```bash
# 检查敏感信息
grep -r "password" . --exclude-dir=node_modules --exclude-dir=target
grep -r "secret" . --exclude-dir=node_modules --exclude-dir=target
grep -r "key" . --exclude-dir=node_modules --exclude-dir=target

# 使用环境变量
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret
```

### 依赖安全

```bash
# 检查依赖漏洞
npm audit
mvn dependency:check

# 更新依赖
npm update
mvn versions:use-latest-releases
```

## 📞 支持

如有问题，请通过以下方式联系：

- 📧 Email: your-email@example.com
- 🐛 Issues: [GitHub Issues](https://github.com/your-username/C-CodeLab/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/your-username/C-CodeLab/discussions)
