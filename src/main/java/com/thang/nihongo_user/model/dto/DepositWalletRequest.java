package com.thang.nihongo_user.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositWalletRequest {

    @NotNull
    @DecimalMin(value = "1000.00", message = "Số tiền nạp tối thiểu là 1.000")
    @DecimalMax(value = "100000000", message = "Số tiền nạp tối đa là 100.000.000đ")
    @Digits(integer = 9, fraction = 0, message = "Số tiền phải là số nguyên VNĐ")
    private BigDecimal amount;

    @Size(max = 500)
    private String description;

    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String requestKey;
}
