package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;

/**
 * 检查服务，提供文本或章节质量检查能力。
 */
public interface CheckService {

    /**
     * 运行一次质量检查并返回检查结果。
     */
    CheckResponse runCheck(CheckRequest request);
}
