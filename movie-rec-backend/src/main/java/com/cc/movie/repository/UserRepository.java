package com.cc.movie.repository;

import com.cc.movie.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@EnableMongoRepositories
// 第一个泛型告诉数据库操作的实体类， 第二个指名该是实体类的 主键 类型
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String name);

    Optional<User> findByUid(int uid);
}
