package com.thang.nihongo_user.model.dto;

import jakarta.validation.constraints.NotBlank;

public class JapaneseAiRequest {

    @NotBlank
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