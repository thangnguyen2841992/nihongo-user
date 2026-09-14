package com.thang.nihongo_user.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositWalletRequest {

    @NotNull
    @DecimalMin(value = "1000.00", message = "Số tiền nạp tối thiểu là 1.000")
    private BigDecimal amount;

    private String description;
}
