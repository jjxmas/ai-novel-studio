package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaUpdateRequest;
import java.util.List;

public interface IdeaService {

    List<IdeaResponse> generateIdeas(IdeaGenerateRequest request);

    List<IdeaResponse> listIdeas(Long projectId);

    void updateIdea(Long ideaId, IdeaUpdateRequest request);

    IdeaResponse rewriteIdea(Long ideaId, IdeaRewriteRequest request);

    void selectIdea(Long ideaId);

    void deleteIdea(Long ideaId);
}
