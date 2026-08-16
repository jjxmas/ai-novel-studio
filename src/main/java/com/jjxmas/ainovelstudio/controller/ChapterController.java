package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterCatalogResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterPageResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterStreamEvent;
import com.jjxmas.ainovelstudio.service.ChapterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/chapters/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("CHAPTER_MODULE_READY");
    }

    @GetMapping("/projects/{projectId}/chapters")
    public ApiResponse<ChapterPageResponse> listChapters(
            @PathVariable Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(chapterService.listChapters(projectId, keyword, page, size));
    }

    @GetMapping("/projects/{projectId}/chapters/catalog")
    public ApiResponse<List<ChapterCatalogResponse>> listChapterCatalog(@PathVariable Long projectId) {
        return ApiResponse.success(chapterService.listChapterCatalog(projectId));
    }

    @GetMapping("/chapters/{chapterId}")
    public ApiResponse<ChapterResponse> getChapter(@PathVariable Long chapterId) {
        return ApiResponse.success(chapterService.getChapter(chapterId));
    }

    @PostMapping("/chapters/{chapterId}/confirm-outline")
    public ApiResponse<Void> confirmChapterOutline(@PathVariable Long chapterId) {
        chapterService.confirmChapterOutline(chapterId);
        return ApiResponse.success("CHAPTER_OUTLINE_CONFIRMED", null);
    }

    @PostMapping("/chapters/{chapterId}/generate-content")
    public ApiResponse<ChapterResponse> generateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterGenerateRequest request) {
        request.setChapterId(chapterId);
        return ApiResponse.success("CHAPTER_CONTENT_GENERATED", chapterService.generateChapter(request));
    }

    @PostMapping(value = "/chapters/{chapterId}/generate-content/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChapterStreamEvent> streamGenerateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterGenerateRequest request) {
        request.setChapterId(chapterId);
        return chapterService.streamGenerateChapter(request);
    }

    @PatchMapping({"/chapters/{chapterId}", "/chapters/{chapterId}/content"})
    public ApiResponse<ChapterResponse> updateChapterContent(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterContentUpdateRequest request) {
        return ApiResponse.success("CHAPTER_CONTENT_SAVED", chapterService.updateChapterContent(chapterId, request));
    }

    @PostMapping({"/chapters/{chapterId}/rewrite-content", "/chapters/{chapterId}/regenerate-content"})
    public ApiResponse<ChapterResponse> rewriteChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterRewriteRequest request) {
        return ApiResponse.success("CHAPTER_CONTENT_REWRITTEN", chapterService.rewriteChapter(chapterId, request));
    }

    @PostMapping(
            value = {"/chapters/{chapterId}/rewrite-content/stream", "/chapters/{chapterId}/regenerate-content/stream"},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChapterStreamEvent> streamRewriteChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterRewriteRequest request) {
        return chapterService.streamRewriteChapter(chapterId, request);
    }
}
