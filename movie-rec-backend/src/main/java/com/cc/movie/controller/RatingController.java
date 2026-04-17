package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Rating;
import com.cc.movie.entity.request.MovieRatingRequest;
import com.cc.movie.service.RatingService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/rating")
public class RatingController {
    @Autowired
    private RatingService ratingService;

    @PostMapping("/add")
    public R<String> rateMovie(@Validated @RequestBody MovieRatingRequest request) {
        // 直接从当前线程口袋里掏出经过 JWT 安检的 UID
        Integer currentUid = UserHolder.getUid();
        log.info("接收到用户 [{}] 对电影 [{}] 的打分: {}", currentUid, request.getMid(), request.getScore());

        ratingService.saveOrUpdateRating(request.getMid(), request.getScore());

        return R.success("打分成功，推荐引擎已启动！");
    }

    @GetMapping("/my/{mid}")
    public R<Rating> getMyRating(@PathVariable("mid") Integer mid) {
        Rating rating = ratingService.findMyRating(mid);
        return R.success(rating);
    }
}
