package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaUpdateRequest;
import com.jjxmas.ainovelstudio.service.IdeaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class IdeaController {

    private final IdeaService ideaService;

    @GetMapping("/ideas/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("创意模块已就绪");
    }

    @PostMapping("/projects/{projectId}/ideas/generate")
    public ApiResponse<List<IdeaResponse>> generateIdeas(
            @PathVariable Long projectId,
            @Valid @RequestBody IdeaGenerateRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.success("创意生成完成", ideaService.generateIdeas(request));
    }

    @GetMapping("/projects/{projectId}/ideas")
    public ApiResponse<List<IdeaResponse>> listIdeas(@PathVariable Long projectId) {
        return ApiResponse.success(ideaService.listIdeas(projectId));
    }

    @PatchMapping("/ideas/{ideaId}")
    public ApiResponse<Void> updateIdea(
            @PathVariable Long ideaId,
            @Valid @RequestBody IdeaUpdateRequest request) {
        ideaService.updateIdea(ideaId, request);
        return ApiResponse.success("创意修改已保存", null);
    }

    @PostMapping({"/ideas/{ideaId}/rewrite", "/ideas/{ideaId}/regenerate"})
    public ApiResponse<IdeaResponse> rewriteIdea(
            @PathVariable Long ideaId,
            @Valid @RequestBody IdeaRewriteRequest request) {
        return ApiResponse.success("创意重生成完成", ideaService.rewriteIdea(ideaId, request));
    }

    @DeleteMapping("/ideas/{ideaId}")
    public ApiResponse<Void> deleteIdea(@PathVariable Long ideaId) {
        ideaService.deleteIdea(ideaId);
        return ApiResponse.success("创意已删除", null);
    }

    @PostMapping("/ideas/{ideaId}/select")
    public ApiResponse<Void> selectIdea(@PathVariable Long ideaId) {
        ideaService.selectIdea(ideaId);
        return ApiResponse.success("创意已选中", null);
    }
}
