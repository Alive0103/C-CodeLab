# Docker 沙箱代码执行使用说明

## 📋 概述

本项目使用Docker沙箱环境执行C代码，确保代码执行的安全性和隔离性。**Java后端服务和前端在本地运行，只有用户提交的C代码在Docker沙箱容器中执行。**

项目采用**容器池方案**，通过复用长期运行的Docker容器来提高代码执行性能，响应速度从2-3秒提升到0.2-0.5秒。

## 🏗️ 架构说明

```
用户请求 → 本地后端服务 → 容器池（获取容器） → 执行代码 → 归还容器 → 返回结果
                ↓
          (使用原有MySQL和Redis云服务)
```

### 关键组件

1. **本地后端服务**
   - 在本地运行Spring Boot应用（不在Docker中）
   - 连接原有的MySQL和Redis云服务
   - 通过Docker API管理容器池

2. **容器池管理器** (`ContainerPool`)
   - 应用启动时创建容器池（默认5个容器）
   - 管理容器的获取、归还和健康检查
   - 自动清理空闲容器并补充新容器

3. **代码执行沙箱** (`c-codelab-sandbox`)
   - 独立的Ubuntu容器
   - 包含GCC编译器
   - 严格的资源限制和网络隔离
   - 容器长期运行，执行后清理临时文件并归还到池中

### 工作流程

```
应用启动
  ↓
创建容器池（默认5个容器）
  ↓
用户提交代码
  ↓
从池中获取空闲容器
  ↓
复制代码文件到容器（docker cp）
  ↓
在容器中执行代码（docker exec）
  ↓
清理容器内临时文件
  ↓
归还容器到池中
  ↓
返回结果
```

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

**注意**：应用启动时会自动创建容器池（约5-10秒），容器池创建完成后才能正常执行代码。

### 3. 启动前端服务（本地）

```bash
cd front
npm install
npm run dev
```

前端服务将运行在：http://localhost:5173

## ⚙️ 配置说明

### 后端配置

在 `application.yml` 中配置：

```yaml
code:
  tempDir: ${java.io.tmpdir}/code-env              # 本地临时文件目录
  sandboxImage: c-codelab-sandbox:latest          # 沙箱镜像名称
  dockerTimeout: 15                                # Docker操作超时（秒）
  containerPool:
    size: 5              # 容器池大小（默认5个）
    maxIdleTime: 300     # 容器最大空闲时间（秒），超过此时间会被清理并重新创建
```

### 配置参数说明

- **size**: 容器池大小
  - 建议值：5-10（根据并发需求调整）
  - 太小：并发时可能等待
  - 太大：占用资源多

- **maxIdleTime**: 容器最大空闲时间
  - 默认：300秒（5分钟）
  - 空闲过久的容器会被清理并重新创建，确保环境干净

### 配置建议

根据并发需求调整容器池大小：

- **低并发**（<5个同时请求）: `size: 3-5`
- **中并发**（5-20个同时请求）: `size: 5-10`
- **高并发**（>20个同时请求）: `size: 10-20`

## 🔧 工作原理

### 容器池初始化

应用启动时（`@PostConstruct`）：
- 创建指定数量的容器
- 容器保持运行状态（`tail -f /dev/null`）
- 初始化容器环境（创建目录、设置权限）

### 代码执行流程

1. **获取容器**：从池中获取空闲容器（阻塞等待，最多10秒）
2. **健康检查**：验证容器是否正常运行
3. **复制文件**：使用 `docker cp` 复制代码文件到容器
4. **执行代码**：使用 `docker exec` 在容器中执行编译和运行
5. **清理文件**：删除容器内的临时文件
6. **归还容器**：容器返回池中，供下次使用

### Docker命令示例

实际执行的Docker命令流程：

```bash
# 1. 从容器池获取容器（应用启动时已创建）
# 容器名称：codelab_pool_1, codelab_pool_2, ...

# 2. 复制代码文件到容器
docker cp /path/to/code_xxx.c codelab_pool_1:/app/code/code_xxx.c

# 3. 在容器中执行代码（容器已在运行）
docker exec codelab_pool_1 /bin/bash -c "
  mkdir -p /app/code &&
  chmod 777 /app && chmod 777 /app/code &&
  cd /app/code &&
  gcc -o program code_xxx.c -Wall -Wextra 2>&1 &&
  chmod +x program &&
  bash -c '/app/code/program' 2>&1 || exit $?
"

# 4. 清理容器内临时文件
docker exec codelab_pool_1 rm -rf /app/code/*

# 5. 容器归还到池中（继续运行，供下次使用）
```

## 🚀 性能优势

### 对比每次创建新容器

| 指标 | 每次创建新容器 | 容器池方案 |
|------|---------------|-----------|
| **容器获取时间** | ~1-2秒 | ~0.1秒 |
| **总执行时间** | 代码时间 + 2-3秒 | 代码时间 + 0.2-0.5秒 |
| **并发能力** | 受创建速度限制 | 受池大小限制 |

### 性能提升

- **响应速度提升**：约 **2-3秒** → **0.2-0.5秒**
- **吞吐量提升**：可同时处理多个请求（取决于池大小）

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

### 安全性说明

- 每次执行后会自动清理容器内的文件
- 容器是复用的，理论上可能存在残留
- 空闲容器会被定期清理和重新创建（`maxIdleTime`）
- 如果对安全性要求极高，可以考虑混合方案

## 🧪 测试说明

### 运行测试

#### 方式 1: 直接运行测试类（推荐）

**容器池会自动初始化，无需手动启动！**

1. **确保环境准备**：
   ```bash
   # 1. 确保 Docker 已启动
   docker ps
   
   # 2. 确保沙箱镜像已构建
   docker images | grep c-codelab-sandbox
   # 如果没有，执行：
   docker build -t c-codelab-sandbox:latest ./code-sandbox
   ```

2. **在 IDE 中运行测试**：
   - 打开 `backend/src/test/java/com/codelab/application/DockerCodeExecutionServiceTest.java`
   - 直接运行整个测试类，或者运行单个测试方法
   - **Spring Boot Test 会自动启动应用上下文，容器池会在 `@PostConstruct` 时自动初始化**

3. **查看测试结果**：
   - 测试会显示容器池状态
   - 显示代码执行结果
   - 显示详细的输出信息

#### 方式 2: 使用 Maven 运行

```bash
cd backend
mvn test -Dtest=DockerCodeExecutionServiceTest -Ddocker.test.enabled=true
```

#### 方式 3: 启动完整应用测试

```bash
cd backend
mvn spring-boot:run
```

应用启动时会自动：
- 初始化容器池（创建5个容器，约5-10秒）
- 容器保持运行状态

然后通过 API 测试代码执行功能。

### 测试内容

测试类包含以下测试：

1. **容器池状态检查** - 验证容器池是否正常初始化
2. **Hello World** - 基本代码执行
3. **简单计算** - 验证输出
4. **编译错误** - 验证错误处理
5. **运行时错误** - 验证异常处理
6. **代码长度限制** - 验证输入验证

### 查看容器池状态

```bash
# 查看容器池中的容器
docker ps | grep codelab_pool

# 应该看到类似：
# codelab_pool_1
# codelab_pool_2
# codelab_pool_3
# ...
```

## 📊 监控和调试

### 获取容器池状态

可以通过 `ContainerPool.getPoolStatus()` 获取：
- 总容器数
- 可用容器数
- 使用中的容器数

### 日志

容器池会输出以下日志：
- 初始化：容器池创建和初始化
- 获取/归还：容器的获取和归还
- 清理：空闲容器的清理和补充

### 查看容器日志

```bash
docker logs codelab_pool_1
```

### 手动检查容器

```bash
# 进入容器
docker exec -it codelab_pool_1 /bin/bash

# 查看目录
ls -la /app/code
```

## ⚠️ 注意事项

### Docker必须运行

后端服务在本地运行，需要通过Docker API访问Docker daemon：

- **Windows**：确保Docker Desktop正在运行
- **Linux/Mac**：确保Docker服务正在运行

### 数据库和Redis

- **MySQL/TiDB**：使用原有的TiDB Cloud配置
- **Redis**：使用原有的Redis Cloud配置
- 这些服务**不在Docker容器中运行**，直接连接云服务

### 资源占用

- 容器池会长期占用内存（每个容器约128MB）
- 默认5个容器 ≈ 640MB 内存
- 根据实际情况调整池大小

### 首次启动

- 容器池初始化需要时间（约5-10秒）
- 容器池创建完成后才能正常执行代码

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

### 3. 容器池初始化失败

**检查**：
- Docker 是否运行
- 镜像是否存在: `docker images | grep c-codelab-sandbox`
- 查看应用日志

### 4. 无法获取容器

**可能原因**：
- 容器池已满，所有容器都在使用
- 容器创建失败

**解决方案**：
- 增加池大小
- 检查 Docker 是否正常运行
- 检查容器镜像是否存在

### 5. 容器执行失败

**可能原因**：
- 容器健康检查失败
- 容器内环境异常

**解决方案**：
- 容器会自动被清理和重新创建
- 检查容器日志：`docker logs <container_name>`

### 6. 代码执行超时

**调整超时时间**：
```yaml
code:
  dockerTimeout: 30  # 增加到30秒
```

### 7. 性能没有提升

**可能原因**：
- 代码执行时间本身很长，容器获取时间占比小
- 容器池太小，经常等待

**解决方案**：
- 增加容器池大小
- 检查是否有其他瓶颈

## 🔄 更新和维护

### 更新沙箱镜像

```bash
docker build -t c-codelab-sandbox:latest ./code-sandbox
```

更新后需要重启后端服务，容器池会使用新镜像重新创建容器。

### 清理容器池

如果容器未正确清理，可以手动清理：

```bash
# 清理所有容器池中的容器
docker ps -a | grep codelab_pool | awk '{print $1}' | xargs docker rm -f

# 或者清理所有codelab_开头的容器
docker ps -a | grep codelab_ | awk '{print $1}' | xargs docker rm -f
```

### 定期重启应用

建议每天重启一次，确保容器环境干净，或者设置较短的 `maxIdleTime`。

## 📚 相关文件

- `code-sandbox/Dockerfile` - 沙箱镜像定义
- `backend/src/main/java/com/codelab/application/DockerCodeExecutionService.java` - Docker沙箱执行服务
- `backend/src/main/java/com/codelab/infrastructure/docker/ContainerPool.java` - 容器池管理器
- `backend/src/test/java/com/codelab/application/DockerCodeExecutionServiceTest.java` - 测试类
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
6. **定期清理**：设置合理的 `maxIdleTime`，定期清理容器

## 📝 最佳实践

1. **根据并发需求调整池大小**
   - 低并发（<5）：池大小 3-5
   - 中并发（5-20）：池大小 5-10
   - 高并发（>20）：池大小 10-20

2. **监控容器池状态**
   - 定期检查可用容器数
   - 如果经常为0，考虑增加池大小

3. **定期重启应用**
   - 建议每天重启一次，确保容器环境干净
   - 或者设置较短的 `maxIdleTime`
