# 清理 codelab_pool 容器的 PowerShell 脚本
docker ps -a --filter "name=codelab_pool" --format "{{.ID}}" | ForEach-Object {
    docker rm -f $_
    Write-Host "已删除容器: $_"
}
Write-Host "清理完成！"

