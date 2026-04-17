package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Movie;
import com.cc.movie.service.UserHistoryService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/history")
public class UserHistoryController {

    @Autowired
    UserHistoryService historyService;

    // 获取历史列表
    @GetMapping("/list")
    public R<List<Movie>> getHistory() {
        Integer uid = UserHolder.getUid();
        return R.success(historyService.getHistoryList(uid));
    }

    // 添加记录
    @PostMapping("/add/{mid}")
    public R<Void> addHistory(@PathVariable Integer mid) {
        Integer uid = UserHolder.getUid();
        historyService.addHistory(uid, mid);
        return R.success(null);
    }
}
