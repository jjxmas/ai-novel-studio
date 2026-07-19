package com.jjxmas.ainovelstudio.module.export.service;

import com.jjxmas.ainovelstudio.module.export.dto.ExportRequest;
import com.jjxmas.ainovelstudio.module.export.dto.ExportResponse;

public interface ExportService {

    ExportResponse exportProject(ExportRequest request);
}

