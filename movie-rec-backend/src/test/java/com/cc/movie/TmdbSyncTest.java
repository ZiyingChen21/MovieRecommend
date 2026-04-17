package com.cc.movie;

import com.cc.movie.service.impl.TmdbSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TmdbSyncTest {

    @Autowired
    private TmdbSyncService tmdbSyncService;

    @Test
    public void runPosterSync() {
        tmdbSyncService.syncAllMoviePosters();
    }
}