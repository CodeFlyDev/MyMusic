package com.mymusic.mapper;

import com.mymusic.entity.Feedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeedbackMapper {

    @Select("SELECT * FROM feedback ORDER BY resolved ASC, id DESC")
    List<Feedback> findAll();

    @Select("SELECT * FROM feedback WHERE id = #{id}")
    Feedback findById(@Param("id") Long id);

    @Insert("INSERT INTO feedback(content, contact, resolved) VALUES(#{content}, #{contact}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Feedback feedback);

    @Update("UPDATE feedback SET resolved = #{resolved}, resolved_at = #{resolvedAt} WHERE id = #{id}")
    int updateResolved(@Param("id") Long id, @Param("resolved") Integer resolved, @Param("resolvedAt") java.time.LocalDateTime resolvedAt);

    @Delete("DELETE FROM feedback WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
