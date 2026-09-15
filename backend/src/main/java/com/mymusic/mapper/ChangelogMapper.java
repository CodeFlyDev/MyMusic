package com.mymusic.mapper;

import com.mymusic.entity.Changelog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChangelogMapper {

    @Select("SELECT * FROM changelogs ORDER BY release_date DESC")
    List<Changelog> findAll();

    @Select("SELECT * FROM changelogs WHERE id = #{id}")
    Changelog findById(@Param("id") Long id);

    @Insert("INSERT INTO changelogs(version, content, release_date) VALUES(#{version}, #{content}, #{releaseDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Changelog changelog);

    @Update("UPDATE changelogs SET version = #{version}, content = #{content}, release_date = #{releaseDate} WHERE id = #{id}")
    int update(Changelog changelog);

    @Delete("DELETE FROM changelogs WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}