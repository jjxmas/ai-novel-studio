package com.jjxmas.ainovelstudio.common.exception;

import lombok.Getter;

/**
 * 第一阶段只保留基础错误码，后续接口文档再细化模块错误。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAMETER_ERROR(40001, "参数错误"),
    NOT_FOUND(40002, "资源不存在"),
    WORKFLOW_GATE_NOT_MET(40003, "流程门禁未满足"),
    MODEL_CONFIG_INVALID(40004, "模型配置无效或未配置 API Key"),
    AI_TASK_UNAVAILABLE(40005, "AI 任务未接入或模型不可用"),
    EXPORT_FAILED(40006, "导出失败"),
    BUSINESS_ERROR(40900, "业务处理失败"),
    SYSTEM_ERROR(50000, "系统异常");

    private final int code;

    private final String message;

    /**
     * 创建错误码枚举项并保存业务码和默认消息。
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
