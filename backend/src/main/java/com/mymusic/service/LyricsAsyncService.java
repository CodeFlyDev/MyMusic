package com.mymusic.service;

import com.mymusic.entity.Song;
import com.mymusic.mapper.SongMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 歌词异步补抓：上传响应返回后在后台联网匹配歌词并入库，
 * 失败静默（歌词保持为空，前端展示"暂无歌词"），不影响上传与播放。
 */
@Service
public class LyricsAsyncService {

    private static final Logger log = LoggerFactory.getLogger(LyricsAsyncService.class);

    private final SongMapper songMapper;
    private final NetEaseClient netEaseClient;

    public LyricsAsyncService(SongMapper songMapper, NetEaseClient netEaseClient) {
        this.songMapper = songMapper;
        this.netEaseClient = netEaseClient;
    }

    @Async
    public void fetchAndSave(Long songId) {
        try {
            Song song = songMapper.findById(songId);
            if (song == null || !isBlank(song.getLyrics())) {
                return;
            }
            String artist = song.getArtist();
            boolean noArtist = isBlank(artist) || "未知歌手".equals(artist);
            String query = noArtist ? song.getTitle() : artist + " " + song.getTitle();

            List<Long> ids = netEaseClient.findSongIds(query, 5);
            if (ids.isEmpty()) {
                log.info("未匹配到网易云歌曲，歌词为空: {}", query);
                return;
            }
            String lrc = netEaseClient.findLyrics(ids);
            if (isBlank(lrc)) {
                log.info("网易云无歌词: {}", query);
                return;
            }
            songMapper.updateLyrics(songId, lrc);
            log.info("已联网匹配歌词: {}", query);
        } catch (Exception e) {
            log.warn("异步匹配歌词失败: songId={}", songId, e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
