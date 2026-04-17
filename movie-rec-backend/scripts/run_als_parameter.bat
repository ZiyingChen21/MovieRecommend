@echo off
chcp 65001 >nul

echo ===================================================
echo 开始提交 Spark ALSParameter 更新任务：%date% %time%
echo ===================================================

spark-submit ^
  --master local[*] ^
  --class com.cc.ALSParameterModel ^
  --driver-memory 4g ^
  --executor-memory 4g ^
  D:\gitClone\MovieRecommend\movie-rec-backend\jars\OfflineLFMRecommend-1.0-SNAPSHOT.jar


if %ERRORLEVEL% EQU 0 (
    echo Spark 任务执行成功！
    exit /b 0
) else (
    echo Spark 任务执行失败！
    exit /b 1
)