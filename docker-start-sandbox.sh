#!/bin/bash

echo "========================================"
echo "C-CodeLab Docker 沙箱环境启动脚本"
echo "========================================"
echo ""

echo "[1/4] 检查Docker环境..."
if ! command -v docker &> /dev/null; then
    echo "[错误] 未检测到Docker，请先安装Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "[错误] 未检测到Docker Compose"
    exit 1
fi

echo "[✓] Docker环境检查通过"
echo ""

echo "[2/4] 构建代码执行沙箱镜像..."
docker build -t c-codelab-sandbox:latest ./code-sandbox
if [ $? -ne 0 ]; then
    echo "[错误] 沙箱镜像构建失败"
    exit 1
fi
echo "[✓] 沙箱镜像构建成功"
echo ""

echo "[3/4] 构建并启动服务..."
docker-compose up -d --build

if [ $? -ne 0 ]; then
    echo "[错误] 服务启动失败"
    exit 1
fi

echo "[✓] 服务启动成功"
echo ""

echo "[4/4] 等待服务就绪..."
sleep 10

echo ""
echo "========================================"
echo "启动完成！"
echo "========================================"
echo ""
echo "前端地址: http://localhost"
echo "后端API:  http://localhost:8081"
echo ""
echo "注意：代码执行在Docker沙箱中进行"
echo "数据库和Redis使用原有云服务配置"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: docker-compose down"
echo ""

