package com.codelab.interfaces.web;

import com.codelab.infrastructure.common.ApiResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName;
            String errorMessage = error.getDefaultMessage();

            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldName = fieldError.getField();
            } else {
                fieldName = error.getObjectName();
            }

            errors.put(fieldName, errorMessage);
        });

        log.warn("参数验证失败: {}", errors);
        return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "参数验证失败: " + errors);
    }


    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleGeneralException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error(ApiResponseCode.INTERNAL_SERVER_ERROR, "服务器内部错误: " + e.getMessage());
    }
}