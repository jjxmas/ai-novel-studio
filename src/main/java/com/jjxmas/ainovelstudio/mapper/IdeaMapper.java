package com.jjxmas.ainovelstudio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IdeaMapper extends BaseMapper<Idea> {

    @Select("SELECT * FROM ideas WHERE id = #{ideaId} FOR UPDATE")
    Idea selectByIdForUpdate(@Param("ideaId") Long ideaId);
}
