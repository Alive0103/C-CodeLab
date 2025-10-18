package com.codelab.interfaces.web;

import com.codelab.infrastructure.common.ApiResponseCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;


    public ApiResponse(int code, String message,T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(ApiResponseCode code,T Data){
        this.code = code.getCode();
        this.message = code.getMessage();
        this.data = Data;
    }

    public ApiResponse(ApiResponseCode code,String message){
        this.code = code.getCode();
        this.message = message;
        this.data = null;
    }


    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ApiResponseCode.SUCCESS, data);
    }

    public static <T> ApiResponse<T> error(ApiResponseCode code, String message) {return new ApiResponse<>(code,message);}
    public static <T> ApiResponse<T> error(ApiResponseCode code, T data) {return new ApiResponse<>(code,data);}

}


