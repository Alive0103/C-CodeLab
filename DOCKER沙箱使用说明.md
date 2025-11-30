# Docker 沙箱代码执行使用说明

## 📋 概述

本项目使用Docker沙箱环境执行C代码，确保代码执行的安全性和隔离性。**Java后端服务和前端在本地运行，只有用户提交的C代码在Docker沙箱容器中执行。**

## 🏗️ 架构说明

```
用户请求 → 本地后端服务 → Docker沙箱容器 → 执行结果 → 存储到数据库 → 返回给用户
                ↓
          (使用原有MySQL和Redis云服务)
```

### 关键组件

1. **本地后端服务**
   - 在本地运行Spring Boot应用（不在Docker中）
   - 连接原有的MySQL和Redis云服务
   - 通过Docker API创建和管理沙箱容器

2. **代码执行沙箱** (`c-codelab-sandbox`)
   - 独立的Ubuntu容器
   - 包含GCC编译器
   - 严格的资源限制和网络隔离
   - 每次代码执行都创建新的容器，执行后自动清理

## 🚀 快速开始

### 1. 构建沙箱镜像（只需执行一次）

```bash
docker build -t c-codelab-sandbox:latest ./code-sandbox
```

或者使用启动脚本：

```bash
# Windows
.\docker-start-sandbox.bat

# Linux/Mac
chmod +x docker-start-sandbox.sh
./docker-start-sandbox.sh
```

### 2. 启动后端服务（本地）

```bash
cd backend
mvn spring-boot:run
```

后端服务将运行在：http://localhost:8081

### 3. 启动前端服务（本地）

```bash
cd front
npm install
npm run dev
```

前端服务将运行在：http://localhost:5173

## 🔒 安全特性

### 沙箱隔离

每个C代码执行都在独立的Docker容器中进行，具有以下安全限制：

- **网络隔离**：`--network none` - 无网络访问
- **内存限制**：128MB
- **CPU限制**：0.5核
- **进程限制**：最多10个进程
- **只读文件系统**：根文件系统只读
- **临时文件系统**：使用tmpfs，容器销毁后自动清理

### 资源限制

```yaml
--memory 128m        # 内存限制
--cpus 0.5          # CPU限制
--pids-limit 10     # 进程数限制
--read-only         # 只读根文件系统
```

## 📝 配置说明

### 后端配置

在 `application.yml` 中配置：

```yaml
code:
  tempDir: ${java.io.tmpdir}/code-env              # 本地临时文件目录
  sandboxImage: c-codelab-sandbox:latest          # 沙箱镜像名称
  dockerTimeout: 15                                # Docker操作超时（秒）
```

## 🔧 工作原理

### 代码执行流程

1. **接收请求**：本地后端服务接收C代码执行请求
2. **创建临时文件**：将C代码写入本地临时文件（`${java.io.tmpdir}/code-env/code_xxx.c`）
3. **创建容器**：使用Docker API创建隔离的沙箱容器（包含编译和执行命令，但不启动）
4. **复制代码文件**：使用`docker cp`将C代码文件复制到容器的`/app/code/`目录
5. **启动执行**：使用`docker start -a`启动容器，容器内自动执行编译和运行命令
6. **获取输出**：从容器标准输出读取执行结果（包括编译错误或程序输出）
7. **清理资源**：自动停止并删除容器，删除本地临时文件
8. **保存结果**：将执行结果异步保存到数据库（原有MySQL/TiDB云服务）
9. **返回结果**：同步返回执行结果给用户

### Docker命令示例

实际执行的Docker命令流程：

```bash
# 1. 创建容器（不启动，包含编译和执行命令）
docker create \
  --name codelab_xxxxx \
  --network none \
  --memory 128m \
  --cpus 0.5 \
  --pids-limit 10 \
  --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=50m \
  --tmpfs /app:rw,noexec,nosuid,size=50m \
  c-codelab-sandbox:latest \
  /bin/bash -c "cd /app/code && gcc -o program code_xxx.c -Wall -Wextra 2>&1 && timeout 5s ./program 2>&1 || exit $?"

# 2. 复制代码文件到容器
docker cp /path/to/code_xxx.c codelab_xxxxx:/app/code/code_xxx.c

# 3. 启动容器并执行（-a参数附加输出）
docker start -a codelab_xxxxx

# 4. 清理容器（自动执行）
docker stop codelab_xxxxx
docker rm codelab_xxxxx
```

**注意**：编译和执行命令在创建容器时就已经指定，容器启动后会立即执行该命令。

## ⚠️ 注意事项

### Docker必须运行

后端服务在本地运行，需要通过Docker API访问Docker daemon：

- **Windows**：确保Docker Desktop正在运行
- **Linux/Mac**：确保Docker服务正在运行

### 数据库和Redis

- **MySQL/TiDB**：使用原有的TiDB Cloud配置
- **Redis**：使用原有的Redis Cloud配置
- 这些服务**不在Docker容器中运行**，直接连接云服务

### 临时文件清理

代码执行后会自动清理：
- Docker容器（自动删除）
- 本地临时代码文件（自动删除）
- 容器内的所有文件（容器销毁后自动清理）

## 🐛 故障排查

### 1. 沙箱镜像未构建

**错误**：`Error response from daemon: No such image: c-codelab-sandbox:latest`

**解决**：
```bash
docker build -t c-codelab-sandbox:latest ./code-sandbox
```

### 2. Docker未运行

**错误**：`Cannot connect to the Docker daemon`

**解决**：
- Windows：启动Docker Desktop
- Linux：启动Docker服务 `sudo systemctl start docker`
- Mac：启动Docker Desktop

### 3. 容器创建失败

**检查日志**：
- 查看后端服务日志
- 检查Docker是否正常运行

**常见原因**：
- 沙箱镜像不存在
- Docker daemon未运行
- 资源限制过严

### 4. 代码执行超时

**调整超时时间**：
```yaml
code:
  dockerTimeout: 30  # 增加到30秒
```

## 📊 监控和日志

### 查看执行日志

```bash
# 查看Docker容器
docker ps -a | grep codelab_

# 查看容器日志（如果容器还在）
docker logs codelab_xxxxx
```

### 性能监控

```bash
# 查看容器资源使用（执行时）
docker stats codelab_xxxxx
```

## 🔄 更新和维护

### 更新沙箱镜像

```bash
docker build -t c-codelab-sandbox:latest ./code-sandbox
```

更新后无需重启后端服务，下次代码执行时会自动使用新镜像。

### 清理旧容器

如果容器未正确清理，可以手动清理：

```bash
# 清理所有codelab_开头的容器
docker ps -a | grep codelab_ | awk '{print $1}' | xargs docker rm -f
```

## 📚 相关文件

- `code-sandbox/Dockerfile` - 沙箱镜像定义
- `backend/src/main/java/com/codelab/application/DockerCodeExecutionService.java` - Docker沙箱执行服务
- `docker-compose.yml` - 仅用于构建沙箱镜像
- `docker-start-sandbox.bat` / `docker-start-sandbox.sh` - 沙箱镜像构建脚本
- `backend/src/main/resources/application.yml` - 数据库和Redis配置（原有云服务配置）
- `本地运行说明.md` - 完整的本地运行指南

## 🔐 安全建议

1. **定期更新沙箱镜像**：保持基础镜像和GCC版本最新
2. **监控资源使用**：设置告警，防止资源耗尽
3. **日志审计**：记录所有代码执行请求
4. **限制并发**：控制同时执行的容器数量
5. **网络隔离**：确保沙箱容器无网络访问
