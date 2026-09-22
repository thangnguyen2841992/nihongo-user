package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.WalletDeposit;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.service.wallet.IWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/nihongo-user/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
public class WalletController {
    private final IWalletService wallets;
    @Value("${wallet.bank-name:}") private String bankName;
    @Value("${wallet.bank-account:}") private String bankAccount;
    @Value("${wallet.bank-account-name:}") private String bankAccountName;

    @GetMapping
    public WalletResponse wallet(@AuthenticationPrincipal Jwt jwt) { return wallets.getWallet(jwt.getSubject(), jwt.getClaimAsString("email")); }
    @PostMapping("/deposit")
    public WalletDeposit deposit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DepositWalletRequest request) {
        return wallets.deposit(jwt.getSubject(), jwt.getClaimAsString("email"), request);
    }
    @GetMapping("/deposits")
    public List<WalletDeposit> history(@AuthenticationPrincipal Jwt jwt) { return wallets.history(jwt.getSubject()); }
    @GetMapping("/bank-info")
    public Map<String, String> bankInfo() { return Map.of("bankName", bankName, "accountNumber", bankAccount, "accountName", bankAccountName); }
    @GetMapping("/admin/deposits") @PreAuthorize("hasRole('ADMIN')")
    public List<WalletDeposit> requests() { return wallets.requests(); }
    @PostMapping("/admin/deposits/{id}/review") @PreAuthorize("hasRole('ADMIN')")
    public WalletDeposit review(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReviewDepositRequest request) {
        return wallets.review(id, jwt.getSubject(), request);
    }
}
