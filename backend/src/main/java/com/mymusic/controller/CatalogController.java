package com.mymusic.controller;

import com.mymusic.service.SongService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 歌手/专辑聚合浏览接口。
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final SongService songService;

    public CatalogController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/artists")
    public Map<String, Object> artists() {
        return Map.of("code", 0, "data", songService.listArtists());
    }

    @GetMapping("/albums")
    public Map<String, Object> albums() {
        return Map.of("code", 0, "data", songService.listAlbums());
    }
}
