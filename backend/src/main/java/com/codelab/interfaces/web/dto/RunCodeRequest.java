package com.codelab.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RunCodeRequest {
    @NotBlank
    @Size(max = 10 * 1024)
    private String code;
    private String title;
}
