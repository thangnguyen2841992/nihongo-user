package com.thang.nihongo_user.model.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CourseDTO {
    private Long courseId;

    private String courseName;

    private String courseDescription;

    private BigDecimal originalPrice;

    private BigDecimal salePrice;

    private String active;
}
