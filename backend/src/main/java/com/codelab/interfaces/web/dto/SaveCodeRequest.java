package com.codelab.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCodeRequest {
    @NotBlank
    private String title;
    @NotBlank
    @Size(max = 10 * 1024)
    private String codeContent;
    @NotBlank
    private String language; // 'c'
    private boolean isPublic;
}


