package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ExportRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ExportResponse;
import com.jjxmas.ainovelstudio.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导出模块入口"
*/
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exports")
public class ExportController {

    private final ExportService exportService;

    /**
     * 检查导出模块接口是否可用。
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("导出模块已就绪");
    }

    /**
     * 导出指定项目的内容。
     */
    @PostMapping
    public ApiResponse<ExportResponse> exportProject(@Valid @RequestBody ExportRequest request) {
        return ApiResponse.success("导出完成", exportService.exportProject(request));
    }
}
