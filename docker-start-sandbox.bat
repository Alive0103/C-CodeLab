@echo off
chcp 65001 >nul
echo ========================================
echo C-CodeLab 沙箱镜像构建脚本
echo ========================================
echo.
echo 注意：此脚本仅构建沙箱镜像
echo Java后端和前端服务需要在本地运行
echo.

echo [1/2] 检查Docker环境...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到Docker，请先安装Docker Desktop
    pause
    exit /b 1
)

echo [✓] Docker环境检查通过
echo.

echo [2/2] 构建代码执行沙箱镜像...
docker build -t c-codelab-sandbox:latest ./code-sandbox
if %errorlevel% neq 0 (
    echo [错误] 沙箱镜像构建失败
    pause
    exit /b 1
)
echo [✓] 沙箱镜像构建成功
echo.

echo ========================================
echo 构建完成！
echo ========================================
echo.
echo 下一步：
echo 1. 启动后端服务: cd backend ^&^& mvn spring-boot:run
echo 2. 启动前端服务: cd front ^&^& npm run dev
echo.
echo 代码执行将在Docker沙箱中进行
echo 数据库和Redis使用原有云服务配置
echo.
pause

