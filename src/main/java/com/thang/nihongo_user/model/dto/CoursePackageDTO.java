package com.thang.nihongo_user.model.dto;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CoursePackageDTO {

    private Long packageId;

    private String packageName;

    private Integer durationDays;

    private BigDecimal price;
}