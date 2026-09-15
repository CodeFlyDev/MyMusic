package com.mymusic.controller;

import com.mymusic.entity.Song;
import com.mymusic.service.SongService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    // 上传接口口令：请求头 X-Upload-Code 必须与之匹配
    @Value("${app.upload-passcode}")
    private String uploadPasscode;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album) {
        List<Song> songs;
        if (artist != null && !artist.isBlank()) {
            songs = songService.listByArtist(artist.trim());
        } else if (album != null && !album.isBlank()) {
            songs = songService.listByAlbum(album.trim());
        } else {
            songs = songService.list(keyword);
        }
        return Map.of("code", 0, "data", songs);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @RequestParam("file") MultipartFile file) {
        if (uploadPasscode == null || uploadPasscode.isBlank() || !uploadPasscode.equals(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Song song = songService.upload(file);
        return Map.of("code", 0, "data", song, "msg", "上传成功");
    }

    /** 上传页门禁：仅校验口令，口令正确才放行显示上传功能 */
    @GetMapping("/upload/check")
    public Map<String, Object> checkUploadCode(
            @RequestHeader(value = "X-Upload-Code", required = false) String code) {
        if (uploadPasscode != null && !uploadPasscode.isBlank() && uploadPasscode.equals(code)) {
            return Map.of("code", 0);
        }
        return Map.of("code", 401, "msg", "访问口令错误");
    }

    /** 播放心跳：前端每 60 秒调用一次，累计播放满 1 分钟才计数 */
    @PostMapping("/{id}/heartbeat")
    public Map<String, Object> heartbeat(@PathVariable Long id) {
        songService.incrementPlayCount(id);
        return Map.of("code", 0);
    }

    /** 兼容旧版：开始播放时调用，不再直接计数，由心跳机制统计 */
    @PostMapping("/{id}/play")
    public Map<String, Object> play(@PathVariable Long id) {
        return Map.of("code", 0);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id) {
        // 删除属于资源管理操作，与上传一样需要口令
        if (uploadPasscode == null || uploadPasscode.isBlank() || !uploadPasscode.equals(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        songService.delete(id);
        return Map.of("code", 0, "msg", "删除成功");
    }

    // 歌词 ----------------------------------------------------------------

    /** 按需获取歌词原文（LRC 文本），无歌词时 data.lyrics 为 null */
    @GetMapping("/{id}/lyrics")
    public Map<String, Object> lyrics(@PathVariable Long id) {
        // lyrics 允许为 null，不能用 Map.of（它不接受 null 值）
        Map<String, Object> data = new HashMap<>();
        data.put("lyrics", songService.getLyrics(id));
        return Map.of("code", 0, "data", data);
    }

    /** 手动编辑/补充歌词，需要管理口令 */
    @PutMapping("/{id}/lyrics")
    public Map<String, Object> updateLyrics(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (uploadPasscode == null || uploadPasscode.isBlank() || !uploadPasscode.equals(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        songService.updateLyrics(id, body.getOrDefault("lyrics", ""));
        return Map.of("code", 0, "msg", "歌词已保存");
    }
}
