package com.cc.movie.task;

import com.cc.movie.entity.TaskLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 触发 Spark ALS 训练及数据更新
 * */
@Slf4j
@Component
public class RecommendTask {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate; // 注入 MongoTemplate 记录时间

    public static final String DAILY_TASK = "DAILY_REC";
    public static final String MONTHLY_TASK = "MONTHLY_PARAM";

    // 分布式锁释放的 Lua 脚本 保证判断和删除的原子性
    private static final String RELEASE_LOCAL_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) else return 0 end";

    // cron : 每月 1 号凌晨 2:00 执行 参数调优
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runParameterOptimizationJob() {
        // 逻辑与之前的推荐任务一致，只是脚本路径换成调优脚本
        executeTaskFlow(MONTHLY_TASK, "D:\\gitClone\\MovieRecommend\\movie-rec-backend\\scripts\\run_als_parameter.bat");
    }

    // cron : 每天凌晨 3:00 执行
    @Scheduled(cron = "0 0 3 * * ?")
    public void runOfflineRecommendSparkJob() {
        executeTaskFlow(DAILY_TASK, "D:\\gitClone\\MovieRecommend\\movie-rec-backend\\scripts\\run_als.bat");
    }

    private void executeTaskFlow(String taskName, String scriptPath) {
        // 生成当前线程的唯一标识（防止误删锁）
        String lockValue = UUID.randomUUID().toString();
        String lockKey = "lock:task:" + taskName;

        // 1. 尝试获取分布式锁 (SETNX)
        // 期时间设为 2 小时，防止 JVM 宕机导致死锁
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, 2, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            log.info("节点成功抢占到分布式锁，准备执行 Spark 离线计算任务...");
            long startTime = System.currentTimeMillis();

            try {
                executeSparkScript(scriptPath);
                mongoTemplate.save(new TaskLog(taskName, new Date()));
                log.info(" {} 任务执行成功，并已将完成时间记录至 MongoDB！", taskName);
            } catch (Exception e) {
                log.error("Spark 任务执行过程中发生异常：", e);
            } finally {
                // 安全释放锁
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptText(RELEASE_LOCAL_LUA_SCRIPT);
                redisScript.setResultType(Long.class);

                Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
                if (result != null && result > 0) {
                    log.info("Spark 任务执行完毕，分布式锁已成功释放。总耗时: {} ms", System.currentTimeMillis() - startTime);
                }
            }
        } else {
            // 没抢到锁的节点，跳过
            log.info("另一个节点正在执行 Spark 任务，本节点跳过此次调度。");
        }
    }

    private void executeSparkScript(String scriptPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptPath);
        // 将脚本的错误输出流和标准输出流合并，方便看日志
        pb.redirectErrorStream(true);
        Process process = pb.start();

        //InputStreamReader 默认会按 Windows 系统的 GBK 读数据
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[Spark] {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Shell 脚本返回了错误的退出码: " + exitCode);
        }
    }
}
