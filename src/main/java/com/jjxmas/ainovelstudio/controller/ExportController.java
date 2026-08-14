package com.jjxmas.ainovelstudio.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ExportRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ExportResponse;
import com.jjxmas.ainovelstudio.service.ExportService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exports")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("EXPORT_MODULE_READY");
    }

    @PostMapping
    public ApiResponse<ExportResponse> exportProject(@Valid @RequestBody ExportRequest request) {
        return ApiResponse.success("EXPORT_CREATED", exportService.exportProject(request));
    }

    @PostMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadProject(@Valid @RequestBody ExportRequest request) {
        ExportResponse response = exportService.exportProject(request);
        String contentType = "md".equals(response.getFormat()) ? "text/markdown; charset=utf-8" : "text/plain; charset=utf-8";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(response.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(response.getContent().getBytes(StandardCharsets.UTF_8));
    }
}
