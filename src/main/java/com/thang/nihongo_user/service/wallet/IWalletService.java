package com.thang.nihongo_user.service.wallet;

import com.thang.nihongo_user.model.WalletDeposit;
import com.thang.nihongo_user.model.dto.*;
import java.util.List;

public interface IWalletService {
    WalletResponse getWallet(String userId, String email);
    WalletDeposit deposit(String userId, String email, DepositWalletRequest request);
    List<WalletDeposit> history(String userId);
    List<WalletDeposit> requests();
    WalletDeposit review(Long id, String reviewer, ReviewDepositRequest request);
}
