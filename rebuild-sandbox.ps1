# 重新构建沙箱镜像的 PowerShell 脚本
Write-Host "正在构建 Docker 镜像..."
docker build -t c-codelab-sandbox:latest ./code-sandbox
if ($LASTEXITCODE -eq 0) {
    Write-Host "镜像构建成功！"
} else {
    Write-Host "镜像构建失败！"
    exit 1
}

