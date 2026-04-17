package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Tags;
import com.cc.movie.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 为电影添加标签
     * 前端调用示例: POST /tag/add/123?tag=烧脑
     */
    @PostMapping("/add/{mid}")
    public R<String> addTag(@PathVariable Integer mid, @RequestParam String tag) {
        log.info("收用户打标请求: 电影ID {}, 标签内容 {}", mid, tag);
        tagService.addTagToMovie(mid, tag);
        return R.success("标签添加成功，已同步至搜索引擎");
    }

    /**
     * 获取电影的所有标签云
     */
    @GetMapping("/movie/{mid}")
    public R<List<Tags>> getMovieTags(@PathVariable Integer mid) {
        return R.success(tagService.findMovieTags(mid));
    }

    /**
     * 获取用户在该电影下的私有标签
     */
    @GetMapping("/my/{mid}")
    public R<List<Tags>> getMyTags(@PathVariable Integer mid) {
        return R.success(tagService.findMyMovieTags(mid));
    }

    @DeleteMapping("/remove/{mid}")
    public R<String> removeTag(@PathVariable("mid") Integer mid, @RequestParam String tag) {
        tagService.removeByUidAndMidAndTag(mid, tag);
        return R.success("标签已移除");
    }
}