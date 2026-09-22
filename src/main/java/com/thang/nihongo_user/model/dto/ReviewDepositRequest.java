package com.thang.nihongo_user.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ReviewDepositRequest {
    @NotNull private Boolean approve;
    @Size(max = 100) private String bankReference;
    @Size(max = 500) private String note;
}
