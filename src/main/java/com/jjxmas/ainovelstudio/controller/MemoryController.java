package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 记忆接口，负责查询项目级剧情记忆和章节摘要。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemoryController {

    private final ChapterMemoryService chapterMemoryService;

    /**
     * 查询指定项目的全局、高层、中层和近期章节记忆。
     */
    @GetMapping("/projects/{projectId}/memories")
    public ApiResponse<ProjectMemoryResponse> getProjectMemory(@PathVariable Long projectId) {
        return ApiResponse.success(chapterMemoryService.getProjectMemory(projectId));
    }
}
