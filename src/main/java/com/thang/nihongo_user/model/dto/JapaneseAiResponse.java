package com.thang.nihongo_user.model.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JapaneseAiResponse {

    private String translation;

    private String reading;

    private List<Vocabulary> vocabulary;

    private List<Grammar> grammar;

    private String sentenceStructure;

    private List<String> examples;
}