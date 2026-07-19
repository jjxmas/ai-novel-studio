package com.jjxmas.ainovelstudio.module.check.controller;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import com.jjxmas.ainovelstudio.module.check.dto.CheckRequest;
import com.jjxmas.ainovelstudio.module.check.dto.CheckResponse;
import com.jjxmas.ainovelstudio.module.check.service.CheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checks")
public class CheckController {

    private final CheckService checkService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("检查模块已就绪");
    }

    @PostMapping
    public ApiResponse<CheckResponse> runCheck(@Valid @RequestBody CheckRequest request) {
        return ApiResponse.success("检查完", checkService.runCheck(request));
    }
}

