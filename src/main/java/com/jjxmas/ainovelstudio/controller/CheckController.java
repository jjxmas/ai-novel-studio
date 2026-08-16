package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationBatchResponse;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.service.ChapterGenerationBatchService;
import com.jjxmas.ainovelstudio.service.CheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检查接口，负责运行文本或章节质量检查。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checks")
public class CheckController {

    private final CheckService checkService;
    private final ChapterGenerationBatchService batchService;

    /**
     * 检查质量检查模块接口是否可用。
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("检查模块已就绪");
    }

    /**
     * 执行一次质量检查并返回问题列表。
     */
    @PostMapping
    public ApiResponse<CheckResponse> runCheck(@Valid @RequestBody CheckRequest request) {
        if (request.getChapterId() == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "全书检查请使用批次接口");
        }
        return ApiResponse.success("检查完", checkService.runCheck(request));
    }

    @PostMapping("/batches")
    public ApiResponse<ChapterGenerationBatchResponse> createBatch(@Valid @RequestBody CheckRequest request) {
        if (request.getChapterId() != null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "全书检查批次不能指定单个章节");
        }
        return ApiResponse.success("全书检查任务已创建", batchService.createQualityCheckBatch(request));
    }

    @GetMapping("/batches/{batchId}")
    public ApiResponse<ChapterGenerationBatchResponse> getBatch(@PathVariable Long batchId) {
        return ApiResponse.success(batchService.getQualityCheckBatch(batchId));
    }

    @GetMapping("/projects/{projectId}/batches/latest")
    public ApiResponse<ChapterGenerationBatchResponse> getLatestBatch(@PathVariable Long projectId) {
        return ApiResponse.success(batchService.getLatestQualityCheckBatch(projectId));
    }
}
