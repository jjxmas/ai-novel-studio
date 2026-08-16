package com.jjxmas.ainovelstudio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    @Select("SELECT * FROM chapters WHERE id = #{chapterId} FOR UPDATE")
    Chapter selectByIdForUpdate(@Param("chapterId") Long chapterId);
}
