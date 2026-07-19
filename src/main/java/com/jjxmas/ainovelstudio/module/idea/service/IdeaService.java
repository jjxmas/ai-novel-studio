package com.jjxmas.ainovelstudio.module.idea.service;

import com.jjxmas.ainovelstudio.module.idea.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaUpdateRequest;
import java.util.List;

public interface IdeaService {

    List<IdeaResponse> generateIdeas(IdeaGenerateRequest request);

    List<IdeaResponse> listIdeas(Long projectId);

    IdeaResponse updateIdea(Long ideaId, IdeaUpdateRequest request);

    IdeaResponse rewriteIdea(Long ideaId, IdeaRewriteRequest request);

    IdeaResponse selectIdea(Long ideaId);
}

