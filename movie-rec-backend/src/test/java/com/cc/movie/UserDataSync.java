package com.cc.movie; // 请确保这里的包名和你的启动类所在包名一致

import com.cc.movie.entity.User;
import com.cc.movie.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class UserDataSync {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void generateShadowUsers() {
        log.info("=== 开始执行影子用户同步任务 ===");

        // MongoDB 底层的 distinct
        List<Integer> distinctUids = mongoTemplate.findDistinct(new Query(), "uid", "Rating", Integer.class);
        log.info("共提取到 {} 个独立的评分用户 UID", distinctUids.size());

        // 统一初始密码 "123456" 的 MD5 值
        String defaultPasswordMd5 = DigestUtils.md5DigestAsHex("123456".getBytes());
        long currentTime = System.currentTimeMillis();

        List<User> newUsersBatch = new ArrayList<>();
        int existCount = 0;
        int insertCount = 0;

        // 遍历检查并生成影子用户
        for (Integer uid : distinctUids) {
            if (userRepository.findByUid(uid).isPresent()) {
                existCount++;
                continue;
            }

            // 构建影子用户对象
            User shadowUser = new User();
            shadowUser.setUid(uid);
            shadowUser.setUsername(String.valueOf(uid)); // username 直接用 uid 字符串
            shadowUser.setPassword(defaultPasswordMd5);
            shadowUser.setFirst(false); // 历史打分用户，不算首次登录
            shadowUser.setTimestamp(currentTime);

            newUsersBatch.add(shadowUser);

            // 每凑齐 1000 个用户，执行一次批量插入，防止一次性插太多卡死
            if (newUsersBatch.size() >= 1000) {
                userRepository.saveAll(newUsersBatch);
                insertCount += newUsersBatch.size();
                log.info("已批量插入 {} 个影子用户...", insertCount);
                newUsersBatch.clear(); // 清空集合，准备下一批
            }
        }

        // 把最后一批没满 1000 个的尾部数据插进去
        if (!newUsersBatch.isEmpty()) {
            userRepository.saveAll(newUsersBatch);
            insertCount += newUsersBatch.size();
        }

        log.info("=== 影子用户同步完成！ ===");
        log.info("原始评分用户总数: {}", distinctUids.size());
        log.info("跳过已存在用户数: {}", existCount);
        log.info("成功插入新用户数: {}", insertCount);
    }
}