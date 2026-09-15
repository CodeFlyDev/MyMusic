package com.mymusic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymusic.entity.Song;
import com.mymusic.mapper.SongMapper;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SongService {

    private static final Logger log = LoggerFactory.getLogger(SongService.class);

    private static final List<String> ALLOWED_EXT = List.of("mp3", "flac", "m4a", "wav", "ogg", "ape", "wma");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SongMapper songMapper;
    private final NetEaseClient netEaseClient;
    private final LyricsAsyncService lyricsAsyncService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public SongService(SongMapper songMapper, NetEaseClient netEaseClient, LyricsAsyncService lyricsAsyncService) {
        this.songMapper = songMapper;
        this.netEaseClient = netEaseClient;
        this.lyricsAsyncService = lyricsAsyncService;
    }

    public List<Song> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return songMapper.listAll();
        }
        return songMapper.search(keyword.trim());
    }

    public List<Song> listByArtist(String artist) {
        return songMapper.listByArtist(artist);
    }

    public List<Song> listByAlbum(String album) {
        return songMapper.listByAlbum(album);
    }

    public List<Map<String, Object>> listArtists() {
        // 聚合查询按完整 artist 字段分组；多歌手歌曲（如 "A / B"）需在服务层拆分为独立歌手再合并计数
        List<Map<String, Object>> raw = songMapper.aggregateArtists();
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> row : raw) {
            String full = String.valueOf(row.get("name"));
            int songCount = ((Number) row.get("songCount")).intValue();
            long playCount = ((Number) row.get("playCount")).longValue();
            String coverName = row.get("coverName") == null ? null : String.valueOf(row.get("coverName"));
            for (String name : splitArtists(full)) {
                if (name.isBlank() || "未知歌手".equals(name)) continue;
                Map<String, Object> m = merged.computeIfAbsent(name, k -> {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("name", k);
                    n.put("songCount", 0);
                    n.put("playCount", 0L);
                    n.put("coverName", null);
                    return n;
                });
                m.put("songCount", ((Number) m.get("songCount")).intValue() + songCount);
                m.put("playCount", ((Number) m.get("playCount")).longValue() + playCount);
                if (m.get("coverName") == null && coverName != null) {
                    m.put("coverName", coverName);
                }
            }
        }
        return merged.values().stream()
                .sorted((a, b) -> {
                    int sc = Integer.compare(((Number) b.get("songCount")).intValue(), ((Number) a.get("songCount")).intValue());
                    return sc != 0 ? sc : Long.compare(((Number) b.get("playCount")).longValue(), ((Number) a.get("playCount")).longValue());
                })
                .toList();
    }

    public List<Map<String, Object>> listAlbums() {
        return songMapper.aggregateAlbums();
    }

    public Song upload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不支持的音频格式: " + ext + "（支持 " + ALLOWED_EXT + "）");
        }

        // 音频文件以 uuid 命名落盘，避免重名与中文路径问题
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        // transferTo 的相对路径会解析到临时目录，必须转成绝对路径
        Path songsDir = Paths.get(uploadDir, "songs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(songsDir);
            file.transferTo(songsDir.resolve(storedName).toFile());
        } catch (IOException e) {
            throw new RuntimeException("保存音频文件失败", e);
        }

        Song song = new Song();
        song.setFileName(storedName);
        song.setFileSize(file.getSize());

        // 解析标签；失败时回退到文件名（尝试 "歌手 - 歌名" 格式）
        parseTags(songsDir.resolve(storedName).toFile(), song, baseNameOf(originalName));

        songMapper.insert(song);

        // 歌词联网匹配放后台异步执行，避免拖慢上传响应
        if (isBlank(song.getLyrics())) {
            lyricsAsyncService.fetchAndSave(song.getId());
        }

        return song;
    }

    private void parseTags(File audioFile, Song song, String fallbackBaseName) {
        String title = null;
        String artist = null;
        String album = null;
        Integer duration = null;
        byte[] cover = null;
        String lyrics = null;

        try {
            AudioFile af = AudioFileIO.read(audioFile);
            duration = af.getAudioHeader().getTrackLength();
            Tag tag = af.getTag();
            if (tag != null) {
                title = firstOrNull(tag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE));
                artist = firstOrNull(tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST));
                album = firstOrNull(tag.getFirst(org.jaudiotagger.tag.FieldKey.ALBUM));
                lyrics = firstOrNull(tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS));
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    cover = artwork.getBinaryData();
                }
            }
        } catch (Exception e) {
            log.warn("解析音频标签失败，回退到文件名: {}", audioFile.getName(), e);
        }

        // 标签缺失时回退：文件名按 "歌手 - 歌名" 解析
        if (isBlank(title)) {
            String base = fallbackBaseName;
            if (isBlank(artist) && base.contains(" - ")) {
                int idx = base.indexOf(" - ");
                artist = base.substring(0, idx).trim();
                title = base.substring(idx + 3).trim();
            } else {
                title = base;
            }
        }
        if (isBlank(artist)) {
            artist = "未知歌手";
        }
        if (isBlank(album)) {
            album = "";
        }
        if (duration == null || duration <= 0) {
            duration = 0;
        }

        song.setTitle(title.trim());
        song.setArtist(normalizeArtist(artist));
        song.setAlbum(album.trim());
        song.setDurationSec(duration);
        song.setLyrics(lyrics);

        // 内嵌封面另存为独立文件
        if (cover != null && cover.length > 0) {
            try {
                Path coversDir = Paths.get(uploadDir, "covers").toAbsolutePath().normalize();
                Files.createDirectories(coversDir);
                String coverName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
                Files.write(coversDir.resolve(coverName), cover);
                song.setCoverName(coverName);
            } catch (IOException e) {
                log.warn("保存封面失败", e);
            }
        } else {
            // 音频没有内嵌封面时，按「歌手 + 歌名」联网匹配一张
            fetchCoverOnline(song);
        }
    }

    /** 无内嵌封面时按标题/歌手联网搜索封面：网易云优先，iTunes 兜底，全部失败则保持占位图 */
    private void fetchCoverOnline(Song song) {
        if (isBlank(song.getTitle())) {
            return;
        }
        boolean noArtist = isBlank(song.getArtist()) || "未知歌手".equals(song.getArtist());
        String query = noArtist ? song.getTitle() : song.getArtist() + " " + song.getTitle();

        String picUrl = null;
        try {
            List<Long> ids = netEaseClient.findSongIds(query, 5);
            if (!ids.isEmpty()) {
                picUrl = netEaseClient.findCoverUrl(ids);
            }
            if (picUrl == null) {
                picUrl = searchItunesCover(query);
            }
        } catch (Exception e) {
            log.warn("在线封面搜索失败: {}", query, e);
        }
        if (picUrl == null) {
            log.info("未找到在线封面: {}", query);
            return;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(picUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200 && resp.body().length > 1024) {
                Path coversDir = Paths.get(uploadDir, "covers").toAbsolutePath().normalize();
                Files.createDirectories(coversDir);
                String coverName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
                Files.write(coversDir.resolve(coverName), resp.body());
                song.setCoverName(coverName);
                log.info("已联网匹配封面: {} <- {}", query, picUrl);
            }
        } catch (Exception e) {
            log.warn("下载在线封面失败: {}", picUrl, e);
        }
    }

    /** iTunes Search API 兜底，artworkUrl100 放大到 600x600 */
    private String searchItunesCover(String query) {
        try {
            String term = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                            "https://itunes.apple.com/search?term=" + term + "&entity=song&limit=1"))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode results = MAPPER.readTree(resp.body()).path("results");
            if (results.isArray() && results.size() > 0) {
                String pic = results.get(0).path("artworkUrl100").asText("");
                if (!pic.isBlank()) {
                    return pic.replace("100x100", "600x600");
                }
            }
        } catch (Exception e) {
            log.warn("iTunes 封面搜索失败: {}", query, e);
        }
        return null;
    }

    // 歌词 ----------------------------------------------------------------

    public String getLyrics(Long id) {
        Song song = songMapper.findById(id);
        if (song == null) {
            throw new IllegalArgumentException("歌曲不存在: " + id);
        }
        return songMapper.findLyricsById(id);
    }

    /** 手动编辑/补充歌词，text 传空串表示清空 */
    public void updateLyrics(Long id, String text) {
        Song song = songMapper.findById(id);
        if (song == null) {
            throw new IllegalArgumentException("歌曲不存在: " + id);
        }
        songMapper.updateLyrics(id, text == null || text.isBlank() ? null : text.trim());
    }

    public void delete(Long id) {
        Song song = songMapper.findById(id);
        if (song == null) {
            throw new IllegalArgumentException("歌曲不存在: " + id);
        }
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        deleteQuietly(base.resolve("songs").resolve(song.getFileName()));
        if (song.getCoverName() != null) {
            deleteQuietly(base.resolve("covers").resolve(song.getCoverName()));
        }
        songMapper.deleteById(id);
    }

    public void incrementPlayCount(Long id) {
        songMapper.incrementPlayCount(id);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", path, e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * 归一化多歌手分隔符：把常见的 / 、 ， 、 , & and feat. ft. vs 与 等统一替换为 " / "。
     * 例如 "周杰伦 & 方文山" 或 "林俊杰,邓紫棋" → "周杰伦 / 方文山"。
     */
    private static String normalizeArtist(String artist) {
        if (isBlank(artist)) return "";
        // 先按常见分隔符切分，再用统一分隔符拼接，避免出现多余空格或混用
        String[] parts = artist.trim().split("\\s*(?:/|、|，|,|\\s*&\\s*|\\s+and\\s+|\\s+feat\\.?\\s+|\\s+ft\\.?\\s+|\\s+vs\\.?\\s+|\\s+与\\s+|\\s+和\\s+)\\s*", -1);
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(t);
        }
        return sb.toString();
    }

    /** 把归一化后的 artist 字段拆成独立歌手列表 */
    private static List<String> splitArtists(String artist) {
        if (isBlank(artist)) return List.of();
        return List.of(artist.split("\\s*/\\s*"));
    }

    private static String firstOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String baseNameOf(String name) {
        if (name == null) {
            return "未知歌曲";
        }
        String base = name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base;
    }
}
