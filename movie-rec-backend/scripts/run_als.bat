@echo off
chcp 65001 >nul
:: 解决 CMD 控制台中文乱码问题

echo ===================================================
echo 开始提交 Spark ALS 推荐任务：%date% %time%
echo ===================================================

:: 核心命令：调用 大数据 Jar 包
spark-submit ^
  --master local[*] ^
  --class com.cc.ALSRecommend ^
  --driver-memory 4g ^
  --executor-memory 4g ^
  D:\gitClone\MovieRecommend\movie-rec-backend\jars\OfflineLFMRecommend-1.0-SNAPSHOT.jar

:: 捕获执行结果，反馈给 Spring Boot
if %ERRORLEVEL% EQU 0 (
    echo ✅ Spark 任务执行成功！
    exit /b 0
) else (
    echo ❌ Spark 任务执行失败！
    exit /b 1
)