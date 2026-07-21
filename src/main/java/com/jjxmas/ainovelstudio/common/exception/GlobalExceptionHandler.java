package com.jjxmas.ainovelstudio.common.exception;

import com.jjxmas.ainovelstudio.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理，保证前端拿到一致的错误结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常并返回对应错误码。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.fail(exception.getErrorCode().getCode(), exception.getMessage());
    }

    /**
     * 处理请求体参数校验失败异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(ErrorCode.PARAMETER_ERROR.getCode(), message);
    }

    /**
     * 处理表单或查询参数绑定校验失败异常。
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(ErrorCode.PARAMETER_ERROR.getCode(), message);
    }

    /**
     * 处理单个参数约束校验失败异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResponse.fail(ErrorCode.PARAMETER_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理未预期的系统异常。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR);
    }
}
