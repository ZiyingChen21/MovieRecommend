package com.cc.movie.task;

import com.cc.movie.entity.TaskLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class TaskStartupRunner implements ApplicationRunner {

    @Autowired
    private RecommendTask recommendTask;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("系统启动，检查是否有漏跑的任务...");

        new Thread(() -> {
            // 1. 每月任务检查 (建议先跑参数调优，再跑每日推荐)
            if (isMonthlyNeedCatchUp()) {
                log.info("检测到本月调优任务尚未执行，开始补跑...");
                recommendTask.runParameterOptimizationJob();
            }

            // 2. 每日任务检查
            if (isTaskNeedCatchUp(RecommendTask.DAILY_TASK)) {
                log.info("检测到今日推荐任务尚未执行，开始补跑...");
                recommendTask.runOfflineRecommendSparkJob();
            }
        }, "CatchUp-Thread").start();
    }

    private boolean isTaskNeedCatchUp(String taskName) {
        TaskLog taskLog = mongoTemplate.findById(taskName, TaskLog.class);
        if (taskLog == null) return true;
        
        LocalDate lastDate = taskLog.getLastSuccessTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return lastDate.isBefore(LocalDate.now());
    }

    private boolean isMonthlyNeedCatchUp() {
        TaskLog taskLog = mongoTemplate.findById(RecommendTask.MONTHLY_TASK, TaskLog.class);
        if (taskLog == null) return true;

        LocalDate lastDate = taskLog.getLastSuccessTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        // 如果上次运行的月份小于当前月份，说明这个月还没跑
        return lastDate.getYear() < LocalDate.now().getYear() || lastDate.getMonthValue() < LocalDate.now().getMonthValue();
    }
}