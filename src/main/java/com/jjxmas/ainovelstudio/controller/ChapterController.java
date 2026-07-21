package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.service.ChapterService;
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

/**
 * 章节接口，负责章节大纲确认、正文生成、正文编辑和重写。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChapterController {

    private final ChapterService chapterService;

    /**
     * 检查章节模块接口是否可用。
     */
    @GetMapping("/chapters/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("章节模块已就绪");
    }

    /**
     * 查询指定项目下的章节列表。
     */
    @GetMapping("/projects/{projectId}/chapters")
    public ApiResponse<List<ChapterResponse>> listChapters(@PathVariable Long projectId) {
        return ApiResponse.success(chapterService.listChapters(projectId));
    }

    /**
     * 确认指定章节的大纲。
     */
    @PostMapping("/chapters/{chapterId}/confirm-outline")
    public ApiResponse<ChapterResponse> confirmChapterOutline(@PathVariable Long chapterId) {
        return ApiResponse.success("章节大纲已确认", chapterService.confirmChapterOutline(chapterId));
    }

    /**
     * 根据章节大纲和上下文生成章节正文。
     */
    @PostMapping("/chapters/{chapterId}/generate-content")
    public ApiResponse<ChapterResponse> generateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterGenerateRequest request) {
        request.setChapterId(chapterId);
        return ApiResponse.success("章节正文生成完成", chapterService.generateChapter(request));
    }

    /**
     * 更新指定章节的正文内容。
     */
    @PatchMapping({"/chapters/{chapterId}", "/chapters/{chapterId}/content"})
    public ApiResponse<ChapterResponse> updateChapterContent(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterContentUpdateRequest request) {
        return ApiResponse.success("章节正文修改已保存", chapterService.updateChapterContent(chapterId, request));
    }

    /**
     * 按重写指令重新生成指定章节正文。
     */
    @PostMapping({"/chapters/{chapterId}/rewrite-content", "/chapters/{chapterId}/regenerate-content"})
    public ApiResponse<ChapterResponse> rewriteChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterRewriteRequest request) {
        return ApiResponse.success("章节正文重生成完成", chapterService.rewriteChapter(chapterId, request));
    }
}
