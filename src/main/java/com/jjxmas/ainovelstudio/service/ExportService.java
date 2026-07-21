package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ExportRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ExportResponse;

/**
 * 导出服务，提供项目内容导出能力。
 */
public interface ExportService {

    /**
     * 按请求导出指定项目内容。
     */
    ExportResponse exportProject(ExportRequest request);
}
