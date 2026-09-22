package com.thang.nihongo_user.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class WalletResponse {

    private Long walletId;

    private String userId;

    private BigDecimal balance;
}
