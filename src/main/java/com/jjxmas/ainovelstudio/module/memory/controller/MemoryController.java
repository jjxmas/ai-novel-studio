package com.jjxmas.ainovelstudio.module.memory.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.memory.dto.ProjectMemoryResponse;
import com.jjxmas.ainovelstudio.module.memory.service.ChapterMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemoryController {

    private final ChapterMemoryService chapterMemoryService;

    @GetMapping("/projects/{projectId}/memories")
    public ApiResponse<ProjectMemoryResponse> getProjectMemory(@PathVariable Long projectId) {
        return ApiResponse.success(chapterMemoryService.getProjectMemory(projectId));
    }
}
