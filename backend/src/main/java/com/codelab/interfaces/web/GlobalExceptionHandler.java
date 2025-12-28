package com.codelab.interfaces.web;

import com.codelab.infrastructure.common.ApiResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<String> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ApiResponse.error(ApiResponseCode.METHOD_NOT_ALLOWED, "请求方法不被允许: " + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        // 获取第一个错误信息，优先返回给用户
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        String message = error.getDefaultMessage();
                        // 如果错误信息已经包含字段名，直接返回；否则添加字段名
                        if (message != null && !message.contains(fieldError.getField())) {
                            return fieldError.getField() + ": " + message;
                        }
                        return message != null ? message : "参数验证失败";
                    }
                    return error.getDefaultMessage() != null ? error.getDefaultMessage() : "参数验证失败";
                })
                .findFirst()
                .orElse("参数验证失败");

        log.warn("参数验证失败: {}", errorMessage);
        return ApiResponse.error(ApiResponseCode.BAD_REQUEST, errorMessage);
    }


    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleGeneralException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error(ApiResponseCode.INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
    }
}