package com.thang.nihongo_user.service.wallet;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.wallet.events.WalletEventRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class WalletServiceImpl implements IWalletService {
    private final IUserWalletRepository wallets;
    private final IWalletTransactionRepository transactions;
    private final IWalletDepositRepository deposits;
    private final WalletEventRecorder events;

    @Override @Transactional
    public WalletResponse getWallet(String userId, String email) {
        UserWallet wallet = lockWallet(userId);
        updateContact(wallet, email);
        return WalletResponse.builder().walletId(wallet.getWalletId()).userId(userId).balance(wallet.getBalance()).build();
    }

    @Override @Transactional
    public WalletDeposit deposit(String userId, String email, DepositWalletRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(new BigDecimal("1000")) < 0
                || amount.compareTo(new BigDecimal("100000000")) > 0 || amount.stripTrailingZeros().scale() > 0) {
            throw badRequest("Số tiền phải là số nguyên từ 1.000 đến 100.000.000đ");
        }
        String key;
        try { key = UUID.fromString(request.getRequestKey()).toString(); }
        catch (IllegalArgumentException | NullPointerException e) { throw badRequest("Mã yêu cầu không hợp lệ"); }
        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (description.length() > 500) throw badRequest("Ghi chú tối đa 500 ký tự");
        UserWallet wallet = lockWallet(userId);
        updateContact(wallet, email);
        Optional<WalletDeposit> existing = deposits.findByUserIdAndRequestKey(userId, key);
        if (existing.isPresent()) {
            WalletDeposit previous = existing.get();
            if (previous.getAmount().compareTo(amount) != 0 || !Objects.equals(previous.getDescription(), description)) {
                throw conflict("Mã yêu cầu đã được dùng với nội dung khác");
            }
            return previous;
        }
        WalletDeposit deposit = new WalletDeposit();
        deposit.setUserId(userId);
        deposit.setRequestKey(key);
        deposit.setAmount(amount);
        deposit.setDescription(description);
        // Request creation never credits money.
        WalletDeposit saved = deposits.save(deposit);
        events.record("CREATED", saved, wallet);
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public List<WalletDeposit> history(String userId) {
        return deposits.findTop100ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override @Transactional(readOnly = true)
    public List<WalletDeposit> requests() {
        List<WalletDeposit> result = new ArrayList<>(deposits.findTop100ByStatusOrderByCreatedAtAsc(PaymentStatus.PENDING));
        result.addAll(deposits.findTop100ByStatusNotOrderByCreatedAtDesc(PaymentStatus.PENDING));
        return result;
    }

    @Override @Transactional
    public WalletDeposit review(Long id, String reviewer, ReviewDepositRequest request) {
        if (request.getApprove() == null) throw badRequest("Thiếu quyết định đối soát");
        String owner = deposits.findOwnerById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));
        // Lock wallet before deposit consistently, including simultaneous reviews.
        UserWallet wallet = lockWallet(owner);
        WalletDeposit deposit = deposits.findForUpdate(id).orElseThrow();
        boolean approve = request.getApprove();
        String reference = request.getBankReference() == null ? "" : request.getBankReference().trim().toUpperCase(Locale.ROOT);
        String note = request.getNote() == null ? "" : request.getNote().trim();
        if (approve && !reference.matches("[A-Z0-9][A-Z0-9._:/-]{2,99}")) {
            throw badRequest("Nhập mã giao dịch ngân hàng (3–100 ký tự, không có khoảng trắng)");
        }
        if (!approve && note.isBlank()) throw badRequest("Vui lòng nhập lý do từ chối");
        if (note.length() > 500) throw badRequest("Ghi chú tối đa 500 ký tự");
        if (deposit.getStatus() != PaymentStatus.PENDING) {
            if (approve && deposit.getStatus() == PaymentStatus.SUCCESS && reference.equals(deposit.getBankReference())) return deposit;
            if (!approve && deposit.getStatus() == PaymentStatus.CANCELLED) return deposit;
            throw conflict("Yêu cầu đã được xử lý, vui lòng tải lại danh sách");
        }
        if (approve) {
            if (deposits.existsByBankReference(reference)) throw conflict("Giao dịch ngân hàng này đã được sử dụng");
            BigDecimal before = wallet.getBalance();
            BigDecimal after = before.add(deposit.getAmount());
            if (after.compareTo(new BigDecimal("9999999999999.99")) > 0) throw conflict("Số dư vượt giới hạn lưu trữ");
            deposit.setBankReference(reference);
            wallet.setBalance(after);
            wallets.save(wallet);
            transactions.save(WalletTransaction.builder().walletId(wallet.getWalletId())
                .transactionType(WalletTransactionType.DEPOSIT).amount(deposit.getAmount())
                .balanceBefore(before).balanceAfter(after).referenceType("WALLET_DEPOSIT")
                .referenceId(deposit.getId()).description("NAP" + deposit.getId() + " / " + reference)
                .status(WalletTransactionStatus.SUCCESS).build());
        }
        deposit.setStatus(approve ? PaymentStatus.SUCCESS : PaymentStatus.CANCELLED);
        deposit.setReviewedBy(reviewer);
        deposit.setReviewedAt(LocalDateTime.now());
        deposit.setReviewNote(note);
        // A duplicate bank reference rolls back both balance and ledger.
        WalletDeposit saved = deposits.saveAndFlush(deposit);
        if (wallet.getNotificationEmail() == null || wallet.getNotificationEmail().isBlank()) {
            throw conflict("Ví chưa có email nhận thông báo. Người dùng cần mở lại trang ví trước khi đối soát.");
        }
        events.record(approve ? "APPROVED" : "REJECTED", saved, wallet);
        return saved;
    }

    private void updateContact(UserWallet wallet, String email) {
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw badRequest("Phiên đăng nhập thiếu email hợp lệ. Vui lòng đăng nhập lại.");
        // Only the signed JWT claim reaches this method, never a request-body email.
        wallet.setNotificationEmail(email);
        wallets.save(wallet);
    }

    private UserWallet lockWallet(String userId) {
        if (userId == null || userId.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        wallets.ensureWallet(userId);
        return wallets.findByUserIdForUpdate(userId).orElseThrow();
    }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
