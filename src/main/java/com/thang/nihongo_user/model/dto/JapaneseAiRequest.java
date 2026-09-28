package com.thang.nihongo_user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JapaneseAiRequest {

    @NotBlank
    @Size(max = 2000, message = "Nội dung phân tích không được vượt quá 2.000 ký tự")
    private String text;

    public JapaneseAiRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
