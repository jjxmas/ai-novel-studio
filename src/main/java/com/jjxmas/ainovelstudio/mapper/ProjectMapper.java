package com.jjxmas.ainovelstudio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("SELECT id FROM projects WHERE id = #{projectId} FOR UPDATE")
    Long lockById(@Param("projectId") Long projectId);
}
