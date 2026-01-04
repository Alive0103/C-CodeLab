# 检查Java和Maven版本
Write-Host "=== Java版本检查 ===" -ForegroundColor Cyan
java -version

Write-Host "`n=== Maven版本检查 ===" -ForegroundColor Cyan
mvn -version

Write-Host "`n=== JAVA_HOME环境变量 ===" -ForegroundColor Cyan
if ($env:JAVA_HOME) {
    Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
} else {
    Write-Host "JAVA_HOME未设置" -ForegroundColor Yellow
    Write-Host "提示：如果Maven使用错误的Java版本，请设置JAVA_HOME环境变量" -ForegroundColor Yellow
}

Write-Host "`n=== 检查Maven使用的Java版本 ===" -ForegroundColor Cyan
$mvnJavaVersion = mvn -version | Select-String "Java version"
Write-Host $mvnJavaVersion

