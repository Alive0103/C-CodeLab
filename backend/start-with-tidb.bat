@echo off
echo 启动C-CodeLab后端服务 (TiDB数据库)
echo.
echo 请确保已设置DB_PASSWORD环境变量
echo 如果未设置，请先运行: set DB_PASSWORD=your_actual_password
echo.
pause
mvn spring-boot:run
