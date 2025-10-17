package com.codelab.interfaces.web.annotation;

// PasswordMatchValidator.java

import com.codelab.interfaces.web.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 验证密码和确认密码是否匹配的自定义验证器
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return true; // 允许其他验证处理空值
        }
        return request.getPassword().equals(request.getConfirmPassword());
    }


    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
    }
}
