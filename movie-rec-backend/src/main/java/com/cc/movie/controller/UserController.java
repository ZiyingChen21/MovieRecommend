package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.User;
import com.cc.movie.entity.request.LoginRequest;
import com.cc.movie.entity.request.RegistRequest;
import com.cc.movie.service.UserService;
import com.cc.movie.utils.JwtUtils;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public R<User> login(HttpServletRequest request, @Validated @RequestBody LoginRequest loginRequest) {
        log.info("接收到完整的请求体: {}",loginRequest);
        log.info("用户尝试登录: {}", loginRequest.getUsername());
        log.info("用户尝试登录: {}", loginRequest != null ? loginRequest.getUsername() : "对象本身就是null");
        User user = userService.login(loginRequest);

        if (user == null) {
            return R.error("用户名或密码错误！");
        }

        // 判断依据：loginRequest.getRememberMe()
        Boolean isRemember = loginRequest.getRememberMe() != null ? loginRequest.getRememberMe() : false;

        String token = JwtUtils.generateToken(user.getUid(), isRemember);

        user.setPassword(null);
        return R.success(user).add("token", token);
    }

    @PostMapping("/register")
    public R<String> register(HttpServletRequest request, @Validated @RequestBody RegistRequest registRequest) {
        log.info("用户尝试注册: {}", registRequest.getUsername());
        Boolean res = userService.registerUser(registRequest);
        if (res) {
            return R.success("注册成功!");
        } else {
            return R.error("注册失败");
        }
    }

    @PostMapping("/myId")
    public R<String> testAuth() {
        Integer currentUid = UserHolder.getUid();
        return R.success("欢迎进入 VIP 房间，你的 UID 是: " + currentUid);
    }

    @PostMapping("/updatePrefs")
    public R<String> updatePrefs(@RequestBody List<String> genres) {
        // 从 JWT 解析出的当前用户上下文中获取 uid
        Integer uid = UserHolder.getUid();

        userService.updatePrefGenres(uid, genres);
        return R.success("偏好标签更新成功！");
    }

    @PostMapping("/updateAvatar")
    public R<String> updateAvatar(@RequestBody Map<String, String> params) {
        Integer uid = UserHolder.getUid();
        String avatarUrl = params.get("avatar");
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return R.error("非法的头像参数");
        }
        userService.updateAvatar(uid, avatarUrl);
        return R.success("头像更新成功");
    }
}
