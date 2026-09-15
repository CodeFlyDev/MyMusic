package com.mymusic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 网易云音乐开放接口封装：搜索歌曲 id → 查封面 / 查歌词。
 * 封面匹配与歌词匹配共用同一次搜索结果，避免重复请求。
 */
@Component
public class NetEaseClient {

    private static final Logger log = LoggerFactory.getLogger(NetEaseClient.class);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 搜索歌曲，返回候选歌曲 id 列表（按相关度排序） */
    public List<Long> findSongIds(String query, int limit) {
        try {
            String form = "s=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=1&limit=" + limit;
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://music.163.com/api/search/get"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", "https://music.163.com")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode songs = MAPPER.readTree(resp.body()).path("result").path("songs");
            if (!songs.isArray()) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>();
            for (JsonNode s : songs) {
                long id = s.path("id").asLong();
                if (id > 0) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("网易云搜索失败: {}", query, e);
            return List.of();
        }
    }

    /** 按候选 id 依次查歌曲详情，返回第一个可用的专辑封面 URL（500x500） */
    public String findCoverUrl(List<Long> songIds) {
        for (long id : songIds) {
            try {
                URI uri = URI.create(
                        "https://music.163.com/api/song/detail/?id=" + id + "&ids=%5B" + id + "%5D");
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(8))
                        .header("Referer", "https://music.163.com")
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                String pic = MAPPER.readTree(resp.body())
                        .path("songs").path(0).path("album").path("picUrl").asText("");
                if (!pic.isBlank()) {
                    return pic + "?param=500y500";
                }
            } catch (Exception e) {
                log.warn("网易云歌曲详情获取失败: id={}", id, e);
            }
        }
        return null;
    }

    /** 按候选 id 依次查歌词，返回第一个可用的 LRC 原文 */
    public String findLyrics(List<Long> songIds) {
        for (long id : songIds) {
            try {
                URI uri = URI.create(
                        "https://music.163.com/api/song/lyric?id=" + id + "&lv=-1&kv=-1&tv=-1");
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(8))
                        .header("Referer", "https://music.163.com")
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                String text = MAPPER.readTree(resp.body())
                        .path("lrc").path("lyric").asText("").trim();
                if (!text.isBlank()) {
                    return text;
                }
            } catch (Exception e) {
                log.warn("网易云歌词获取失败: id={}", id, e);
            }
        }
        return null;
    }
}
