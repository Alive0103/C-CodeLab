# TiDB数据库配置完成说明

## ✅ 配置已完成

已成功将应用配置从H2内存数据库改为TiDB云数据库。

## 📋 配置详情

### 数据库连接信息
- **数据库类型**: TiDB (MySQL 8.0兼容)
- **主机地址**: gateway01.ap-northeast-1.prod.aws.tidbcloud.com
- **端口**: 4000
- **数据库名**: c_test
- **用户名**: 2GtHVk27Cjofv3g.root
- **SSL连接**: 启用 (使用isrgrootx1.pem证书)

### 连接池配置
- 最大连接数: 10
- 最小空闲连接: 5
- 连接超时: 30秒
- 空闲超时: 10分钟
- 连接最大生命周期: 30分钟

## 🚀 启动应用

### 方法1: 使用启动脚本 (推荐)
```bash
# Windows
start-with-tidb.bat

# Linux/Mac
chmod +x start-with-tidb.sh
./start-with-tidb.sh
```

### 方法2: 手动设置环境变量

#### Windows PowerShell
```powershell
$env:DB_PASSWORD="your_actual_tidb_password"
mvn spring-boot:run
```

#### Windows CMD
```cmd
set DB_PASSWORD=your_actual_tidb_password
mvn spring-boot:run
```

#### Linux/Mac
```bash
export DB_PASSWORD="your_actual_tidb_password"
mvn spring-boot:run
```

### 方法3: IDE配置
在IDE的运行配置中添加环境变量：
- 变量名: `DB_PASSWORD`
- 变量值: 你的实际TiDB密码

## 🔧 重要配置变更

### application.yml 主要变更
1. **数据源URL**: 从H2内存数据库改为TiDB云数据库
2. **驱动类**: 从H2驱动改为MySQL驱动
3. **用户名**: 使用TiDB提供的用户名
4. **密码**: 通过环境变量 `DB_PASSWORD` 设置
5. **SSL配置**: 启用SSL连接
6. **连接池**: 配置HikariCP连接池参数
7. **方言**: 设置为MySQL8Dialect

### 移除的配置
- 移除了JWT相关配置 (secret, access-expiration, refresh-expiration)

## 🔒 安全注意事项

1. **密码安全**: 
   - 数据库密码通过环境变量设置，不会出现在代码中
   - 请勿将真实密码提交到代码仓库

2. **SSL连接**: 
   - 使用SSL证书确保连接安全
   - 证书文件: `isrgrootx1.pem`

3. **环境变量**: 
   - 使用 `DB_PASSWORD` 环境变量设置数据库密码
   - 默认值: `your_password_here` (需要替换为实际密码)

## 📝 测试步骤

1. **设置环境变量**:
   ```bash
   export DB_PASSWORD="your_actual_password"
   ```

2. **启动应用**:
   ```bash
   mvn spring-boot:run
   ```

3. **检查连接**:
   - 查看控制台日志，确认数据库连接成功
   - 访问 http://localhost:8080 测试应用

4. **测试API**:
   - 注册用户: `POST /api/auth/register`
   - 登录: `POST /api/auth/login`
   - 获取用户信息: `GET /api/user`

## 🐛 故障排除

### 连接失败
1. 检查网络连接
2. 确认密码正确
3. 检查SSL证书文件是否存在
4. 确认防火墙设置

### 环境变量问题
1. 确认 `DB_PASSWORD` 环境变量已设置
2. 检查变量值是否正确
3. 重启终端/IDE

### 编译问题
1. 确认MySQL驱动依赖已包含
2. 检查Java版本兼容性
3. 清理并重新编译: `mvn clean compile`

## 📚 相关文件

- `application.yml`: 主配置文件
- `start-with-tidb.bat`: Windows启动脚本
- `start-with-tidb.sh`: Linux/Mac启动脚本
- `数据库配置说明.md`: 详细配置说明
- `认证系统修改说明.md`: 认证系统修改说明

## ✨ 下一步

1. 设置你的实际TiDB密码到环境变量
2. 启动应用测试数据库连接
3. 测试用户注册和登录功能
4. 验证所有API接口正常工作
