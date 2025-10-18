package com.codelab.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$%!%*#?&])[A-Za-z\\d@$%!%*#?&]{8,}$", 
             message = "密码必须包含字母、数字和特殊字符，长度至少8位")
    private String newPassword;
}
