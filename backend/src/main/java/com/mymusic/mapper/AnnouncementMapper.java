package com.mymusic.mapper;

import com.mymusic.entity.Announcement;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("SELECT * FROM announcements WHERE visible = 1 ORDER BY priority DESC, created_at DESC")
    List<Announcement> findVisible();

    @Select("SELECT * FROM announcements ORDER BY priority DESC, created_at DESC")
    List<Announcement> findAll();

    @Select("SELECT * FROM announcements WHERE id = #{id}")
    Announcement findById(@Param("id") Long id);

    @Insert("INSERT INTO announcements(title, content, priority, visible) VALUES(#{title}, #{content}, #{priority}, #{visible})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement announcement);

    @Update("UPDATE announcements SET title = #{title}, content = #{content}, priority = #{priority}, visible = #{visible}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Announcement announcement);

    @Delete("DELETE FROM announcements WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}