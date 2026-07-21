package com.jjxmas.ainovelstudio.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回体，前端只需要按 code、message、data 判断结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    private Boolean success;

    private Long timestamp;

    private String requestId;

    /**
     * 构造默认成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("成功", data);
    }

    /**
     * 构造带自定义消息的成功响应。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .success(true)
                .timestamp(System.currentTimeMillis())
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * 按错误码构造失败响应。
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 按错误码和消息构造失败响应。
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .timestamp(System.currentTimeMillis())
                .requestId(UUID.randomUUID().toString())
                .build();
    }
}
