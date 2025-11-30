# C代码执行沙箱镜像
# 使用原始镜像名，通过Docker镜像加速器拉取
FROM ubuntu:22.04

# 安装GCC编译器和必要的工具
RUN apt-get update && \
    apt-get install -y \
    gcc \
    coreutils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 创建代码执行目录
RUN mkdir -p /app/code && \
    chmod 777 /app/code

WORKDIR /app/code

# 默认命令
CMD ["/bin/bash"]

