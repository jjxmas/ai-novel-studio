package com.jjxmas.ainovelstudio.module.chapter.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.module.chapter.service.ChapterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/chapters/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("章节模块已就绪");
    }

    @GetMapping("/projects/{projectId}/chapters")
    public ApiResponse<List<ChapterResponse>> listChapters(@PathVariable Long projectId) {
        return ApiResponse.success(chapterService.listChapters(projectId));
    }

    @PostMapping("/chapters/{chapterId}/confirm-outline")
    public ApiResponse<ChapterResponse> confirmChapterOutline(@PathVariable Long chapterId) {
        return ApiResponse.success("章节大纲已确认", chapterService.confirmChapterOutline(chapterId));
    }

    @PostMapping("/chapters/{chapterId}/generate-content")
    public ApiResponse<ChapterResponse> generateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterGenerateRequest request) {
        request.setChapterId(chapterId);
        return ApiResponse.success("章节正文生成完成", chapterService.generateChapter(request));
    }

    @PatchMapping({"/chapters/{chapterId}", "/chapters/{chapterId}/content"})
    public ApiResponse<ChapterResponse> updateChapterContent(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterContentUpdateRequest request) {
        return ApiResponse.success("章节正文修改已保存", chapterService.updateChapterContent(chapterId, request));
    }

    @PostMapping({"/chapters/{chapterId}/rewrite-content", "/chapters/{chapterId}/regenerate-content"})
    public ApiResponse<ChapterResponse> rewriteChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterRewriteRequest request) {
        return ApiResponse.success("章节正文重生成完成", chapterService.rewriteChapter(chapterId, request));
    }
}
