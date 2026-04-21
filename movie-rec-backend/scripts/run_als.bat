@echo off
chcp 65001 >nul
:: 解决 CMD 控制台中文乱码问题

echo ===================================================
echo 开始提交 Spark ALS 推荐任务：%date% %time%
echo ===================================================

set "SPARK_BIN=D:\tool\env\spark2.1.1\spark-2.1.1-bin-hadoop2.7\bin\spark-submit.cmd"

set "MAIN_CLASS=com.cc.ALSRecommend"

set "APP_JAR=D:\gitClone\MovieRecommend\movie-rec-backend\jars\OfflineLFMRecommend-1.0-SNAPSHOT.jar"

spark-submit --master local[*] --class %MAIN_CLASS% --driver-memory 4g --executor-memory 4g %APP_JAR%


:: 捕获执行结果，反馈给 Spring Boot
if %ERRORLEVEL% EQU 0 (
    echo ✅ Spark 任务执行成功！
    exit /b 0
) else (
    echo ❌ Spark 任务执行失败！
    exit /b 1
)