package com.cc.movie.service.impl;

import com.cc.movie.entity.User;
import com.cc.movie.entity.request.LoginRequest;
import com.cc.movie.entity.request.RegistRequest;
import com.cc.movie.repository.UserRepository;
import com.cc.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean registerUser(RegistRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("用户名不可用：该用户名已被注册！");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("输入不一致：两次输入的密码不一致！");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        //user.setPassword(request.getPassword());
        user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes()));
        user.setUid((int) (System.currentTimeMillis() / 1000));
        user.setFirst(true);
        user.setTimestamp(System.currentTimeMillis());
        userRepository.save(user);
        return true;
    }

    @Override
    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            return null;
        }

        if (!user.getPassword().equals(DigestUtils.md5DigestAsHex(request.getPassword().getBytes()))) {
            return null;
        }
        return user;
    }

    @Override
    public User findByUid(int uid) {
        return userRepository.findByUid(uid).orElse(null);
    }

    @Override
    public void updatePrefGenres(int uid, List<String> genres) {
        User user = userRepository.findByUid(uid).orElse(null);
        if (user != null) {
            user.setPrefGenres(genres);
            user.setFirst(false);
            userRepository.save(user);
        }
    }

    @Override
    public void updateAvatar(int uid, String avatarUrl) {
        User user = userRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("用户态异常"));
        user.setAvatar(avatarUrl);
        userRepository.save(user);
    }


}
