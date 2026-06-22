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
public class BookResponse {

    private Long bookId;

    private String bookName;

    private String description;

    private List<ImageDTO> imageUrls;

    private String typeName;

    private String levelName;
}