package com.codelab.interfaces.web.dto;

import com.codelab.interfaces.web.annotation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordMatch(message = "密码和确认密码不匹配")
public class RegisterRequest {
    @NotBlank
    @Size(min = 8, max = 20, message = "用户名长度必须在8-20个字符之间")
    private String username;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$%!%*#?&_])[A-Za-z\\d@$%!%*#?&_]{8,}$", 
             message = "密码必须包含字母、数字和特殊字符(@$%!%*#?&_)，长度至少8位")
    private String password;

    @NotBlank
    private String confirmPassword;

    @Email
    private String email;
}


