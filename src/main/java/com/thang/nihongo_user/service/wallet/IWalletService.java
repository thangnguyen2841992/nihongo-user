package com.thang.nihongo_user.service.wallet;

import com.thang.nihongo_user.model.dto.DepositWalletRequest;
import com.thang.nihongo_user.model.dto.WalletResponse;

public interface IWalletService {
    WalletResponse getWallet(Long userId);
    WalletResponse deposit(Long userId, DepositWalletRequest request);
}
