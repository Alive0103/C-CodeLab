package com.codelab.interfaces.web.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Email(message = "邮箱格式不正确")
    private String email;
}
