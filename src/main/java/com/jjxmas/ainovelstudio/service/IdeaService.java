package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Project;

import java.util.List;

/**
 * 创意服务，提供创意生成、维护、重写和选中能力。
 */
public interface IdeaService {

    /**
     * 按请求为项目生成创意列表。
     */
    List<IdeaResponse> generateIdeas(IdeaGenerateRequest request);

    /**
     * 查询指定项目下的创意列表。
     */
    List<IdeaResponse> listIdeas(Long projectId);

    /**
     * 更新指定创意的人工编辑内容。
     */
    IdeaResponse updateIdea(Long ideaId, IdeaUpdateRequest request);

    /**
     * 按修改指令重写指定创意。
     */
    IdeaResponse rewriteIdea(Long ideaId, IdeaRewriteRequest request);

    /**
     * 将指定创意设为项目选中方案。
     */
    IdeaResponse selectIdea(Long ideaId);

    /**
     * 删除未选中的创意方案。
     */
    void deleteIdea(Long ideaId);
}
