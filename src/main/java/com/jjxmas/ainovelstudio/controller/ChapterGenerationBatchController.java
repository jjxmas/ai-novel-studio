package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchSummaryResponse;
import com.jjxmas.ainovelstudio.service.ChapterGenerationBatchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChapterGenerationBatchController {

    private final ChapterGenerationBatchService batchService;

    @PostMapping("/projects/{projectId}/chapter-generation-batches")
    public ApiResponse<ChapterGenerationBatchResponse> createBatch(
            @PathVariable Long projectId,
            @Valid @RequestBody ChapterGenerationBatchCreateRequest request) {
        return ApiResponse.success("chapter-generation-batch-created", batchService.createBatch(projectId, request));
    }

    @GetMapping("/chapter-generation-batches/{batchId}")
    public ApiResponse<ChapterGenerationBatchResponse> getBatch(@PathVariable Long batchId) {
        return ApiResponse.success(batchService.getBatch(batchId));
    }

    @GetMapping("/projects/{projectId}/chapter-generation-batches")
    public ApiResponse<List<ChapterGenerationBatchSummaryResponse>> listBatches(@PathVariable Long projectId) {
        return ApiResponse.success(batchService.listBatches(projectId));
    }

    @GetMapping("/projects/{projectId}/chapter-generation-batches/latest")
    public ApiResponse<ChapterGenerationBatchResponse> getLatestBatch(@PathVariable Long projectId) {
        return ApiResponse.success(batchService.getLatestBatch(projectId));
    }

    @PostMapping("/chapter-generation-batches/{batchId}/cancel")
    public ApiResponse<ChapterGenerationBatchResponse> cancelBatch(@PathVariable Long batchId) {
        return ApiResponse.success("chapter-generation-batch-cancelled", batchService.cancelBatch(batchId));
    }

    @PostMapping("/chapter-generation-batches/{batchId}/pause")
    public ApiResponse<ChapterGenerationBatchResponse> pauseBatch(@PathVariable Long batchId) {
        return ApiResponse.success("chapter-generation-batch-paused", batchService.pauseBatch(batchId));
    }

    @PostMapping("/chapter-generation-batches/{batchId}/resume")
    public ApiResponse<ChapterGenerationBatchResponse> resumeBatch(@PathVariable Long batchId) {
        return ApiResponse.success("chapter-generation-batch-resumed", batchService.resumeBatch(batchId));
    }

    @PostMapping("/chapter-generation-batches/{batchId}/retry-failed")
    public ApiResponse<ChapterGenerationBatchResponse> retryFailed(@PathVariable Long batchId) {
        return ApiResponse.success("chapter-generation-batch-retried", batchService.retryFailed(batchId));
    }
}
