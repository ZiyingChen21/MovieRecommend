package com.cc.movie.service;

import com.cc.movie.entity.Tags;
import java.util.List;

public interface TagService {
    /**
     * 为电影添加标签，并同步更新 ES 索引
     */
    void addTagToMovie(Integer mid, String tagName);

    /**
     * 获取某部电影的所有标签
     */
    List<Tags> findMovieTags(Integer mid);

    /**
     * 获取当前用户对某部电影打过的标签
     */
    List<Tags> findMyMovieTags(Integer mid);

    void removeByUidAndMidAndTag(Integer mid, String tag);
}