package com.mymusic.mapper;

import com.mymusic.entity.Song;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SongMapper {

    @Select("SELECT * FROM songs WHERE title LIKE CONCAT('%', #{keyword}, '%') OR artist LIKE CONCAT('%', #{keyword}, '%') OR album LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC")
    List<Song> search(@Param("keyword") String keyword);

    @Select("SELECT * FROM songs ORDER BY id DESC")
    List<Song> listAll();

    @Select("SELECT * FROM songs WHERE id = #{id}")
    Song findById(@Param("id") Long id);

    // 多歌手歌曲的 artist 字段以 " / " 分隔；按任一分隔位置匹配该歌手
    @Select("""
            SELECT * FROM songs
            WHERE artist = #{artist}
               OR artist LIKE CONCAT(#{artist}, ' / %')
               OR artist LIKE CONCAT('% / ', #{artist})
               OR artist LIKE CONCAT('% / ', #{artist}, ' / %')
            ORDER BY play_count DESC, id DESC
            """)
    List<Song> listByArtist(@Param("artist") String artist);

    @Select("SELECT * FROM songs WHERE album = #{album} ORDER BY id ASC")
    List<Song> listByAlbum(@Param("album") String album);

    @Insert("INSERT INTO songs(title, artist, album, duration_sec, file_name, cover_name, file_size, play_count, lyrics, created_at) " +
            "VALUES(#{title}, #{artist}, #{album}, #{durationSec}, #{fileName}, #{coverName}, #{fileSize}, 0, #{lyrics}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Song song);

    @Update("UPDATE songs SET play_count = play_count + 1 WHERE id = #{id}")
    int incrementPlayCount(@Param("id") Long id);

    @Delete("DELETE FROM songs WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // 歌词 ----------------------------------------------------------------

    @Select("SELECT lyrics FROM songs WHERE id = #{id}")
    String findLyricsById(@Param("id") Long id);

    @Update("UPDATE songs SET lyrics = #{lyrics} WHERE id = #{id}")
    int updateLyrics(@Param("id") Long id, @Param("lyrics") String lyrics);

    // 聚合：歌手/专辑 -------------------------------------------------------
    // 按歌曲数倒序（让冷门歌手/专辑也能露面），播放数作为次序；封面取该组热度最高且有封面的歌曲
    @Select("""
            SELECT artist AS name,
                   COUNT(*) AS songCount,
                   COALESCE(SUM(play_count), 0) AS playCount,
                   (SELECT s2.cover_name FROM songs s2
                      WHERE s2.artist = s.artist AND s2.cover_name IS NOT NULL AND s2.cover_name <> ''
                      ORDER BY s2.play_count DESC LIMIT 1) AS coverName
            FROM songs s
            WHERE TRIM(artist) <> '' AND artist <> '未知歌手'
            GROUP BY artist
            ORDER BY songCount DESC, playCount DESC
            """)
    List<Map<String, Object>> aggregateArtists();

    @Select("""
            SELECT album AS name,
                   MAX(artist) AS artist,
                   COUNT(*) AS songCount,
                   COALESCE(SUM(play_count), 0) AS playCount,
                   (SELECT s2.cover_name FROM songs s2
                      WHERE s2.album = s.album AND s2.cover_name IS NOT NULL AND s2.cover_name <> ''
                      ORDER BY s2.play_count DESC LIMIT 1) AS coverName
            FROM songs s
            WHERE TRIM(album) <> ''
            GROUP BY album
            ORDER BY songCount DESC, playCount DESC
            """)
    List<Map<String, Object>> aggregateAlbums();
}
