package com.cc.movie.service;

import com.cc.movie.entity.User;
import com.cc.movie.entity.request.LoginRequest;
import com.cc.movie.entity.request.RegistRequest;

import java.util.List;

public interface UserService {
    boolean registerUser(RegistRequest request);

    User login(LoginRequest request);

    User findByUid(int uid);

    // 更新用户偏好标签
    void updatePrefGenres(int uid, List<String> genres);

    void updateAvatar(int uid, String avatarUrl);
}
