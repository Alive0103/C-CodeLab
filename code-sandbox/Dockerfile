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

# 创建非特权用户（UID 1000，GID 1000）
RUN groupadd -r sandbox && \
    useradd -r -g sandbox -u 1000 -d /app -s /bin/bash sandbox && \
    mkdir -p /app/code && \
    chown -R sandbox:sandbox /app && \
    chmod 755 /app && \
    chmod 755 /app/code

# 确保/tmp目录存在且可写
RUN mkdir -p /tmp && chmod 1777 /tmp

WORKDIR /app/code

# 使用非特权用户运行
USER sandbox

# 默认命令
CMD ["/bin/bash"]

