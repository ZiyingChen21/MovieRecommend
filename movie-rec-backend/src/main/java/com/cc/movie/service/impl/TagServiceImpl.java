package com.cc.movie.service.impl;

import com.cc.movie.entity.EsMovie;
import com.cc.movie.entity.Tags;
import com.cc.movie.repository.TagRepository;
import com.cc.movie.service.TagService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void addTagToMovie(Integer mid, String tagName) {
        Integer uid = UserHolder.getUid();

        // 防止重复打标
        List<Tags> existingTags = tagRepository.findByUidAndMid(uid, mid);
        if (existingTags.stream().anyMatch(t -> t.getTag().equals(tagName))) {
            log.info("用户 {} 已经为电影 {} 打过标签 [{}]，不再重复录入", uid, mid, tagName);
            return;
        }

        // 存入 MongoDB
        Tags tag = new Tags();
        tag.setUid(uid);
        tag.setMid(mid);
        tag.setTag(tagName);
        tag.setTimestamp(System.currentTimeMillis() / 1000);

        // 直接使用 Repository 保存，极其简洁
        tagRepository.save(tag);

        // 同步更新 Elasticsearch
        syncTagToEs(mid, tagName);
    }

    private void syncTagToEs(Integer mid, String newTag) {
        try {
            // 将 ES 的所有操作放进 try-catch
            EsMovie esMovie = elasticsearchRestTemplate.get(String.valueOf(mid), EsMovie.class);
            if (esMovie != null) {
                String currentGenres = esMovie.getGenres();
                String updatedGenres = (currentGenres == null || currentGenres.isEmpty())
                        ? newTag : currentGenres + "|" + newTag;

                Document document = Document.create();
                document.put("genres", updatedGenres);

                UpdateQuery updateQuery = UpdateQuery.builder(String.valueOf(mid))
                        .withDocument(document)
                        .withDocAsUpsert(true)
                        .build();

                elasticsearchRestTemplate.update(updateQuery, IndexCoordinates.of("recommend"));
                log.info("用户打标同步成功: 电影 [{}] 追加标签 [{}]", mid, newTag);
            }
        } catch (Exception e) {
            // 关键点：捕获异常并只打日志，不抛出
            log.error("ES同步失败（索引不存在或连接超时），但不影响数据库入库: {}", e.getMessage());
        }
    }

    @Override
    public List<Tags> findMovieTags(Integer mid) {
        return tagRepository.findByMid(mid);
    }

    @Override
    public List<Tags> findMyMovieTags(Integer mid) {
        Integer uid = UserHolder.getUid();
        return tagRepository.findByUidAndMid(uid, mid);
    }

    @Override
    public void removeByUidAndMidAndTag(Integer mid, String tag) {
        Integer uid = UserHolder.getUid();
        tagRepository.deleteByUidAndMidAndTag(uid, mid, tag);
        log.info("用户 {} 移除了电影 {} 的标签: {}", uid, mid, tag);
    }
}