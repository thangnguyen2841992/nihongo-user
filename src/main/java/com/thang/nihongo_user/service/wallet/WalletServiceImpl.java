package com.thang.nihongo_user.service.wallet;


import com.thang.nihongo_user.model.UserWallet;
import com.thang.nihongo_user.model.WalletTransaction;
import com.thang.nihongo_user.model.WalletTransactionStatus;
import com.thang.nihongo_user.model.WalletTransactionType;
import com.thang.nihongo_user.model.dto.DepositWalletRequest;
import com.thang.nihongo_user.model.dto.WalletResponse;
import com.thang.nihongo_user.repository.IUserWalletRepository;
import com.thang.nihongo_user.repository.IWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements IWalletService {

    private final IUserWalletRepository userWalletRepository;

    private final IWalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {

        UserWallet wallet = userWalletRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy ví của user"));

        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse deposit(Long userId, DepositWalletRequest request) {

        BigDecimal amount = request.getAmount();

        if (amount == null) {
            throw new IllegalArgumentException("Số tiền nạp không được để trống");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        UserWallet wallet = userWalletRepository.findByUserIdForUpdate(userId).orElseGet(() -> createWallet(userId));

        BigDecimal balanceBefore = wallet.getBalance();

        if (balanceBefore == null) {
            balanceBefore = BigDecimal.ZERO;
        }

        BigDecimal balanceAfter = balanceBefore.add(amount);

        wallet.setBalance(balanceAfter);

        userWalletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder().walletId(wallet.getWalletId()).transactionType(WalletTransactionType.DEPOSIT).amount(amount).balanceBefore(balanceBefore).balanceAfter(balanceAfter).referenceType("WALLET").referenceId(wallet.getWalletId()).description(request.getDescription()).status(WalletTransactionStatus.SUCCESS).build();

        walletTransactionRepository.save(transaction);

        return toResponse(wallet);
    }

    private UserWallet createWallet(Long userId) {

        UserWallet wallet = UserWallet.builder().userId(userId).balance(BigDecimal.ZERO).build();

        return userWalletRepository.save(wallet);
    }

    private WalletResponse toResponse(UserWallet wallet) {
        return WalletResponse.builder().walletId(wallet.getWalletId()).userId(wallet.getUserId()).balance(wallet.getBalance()).build();
    }
}