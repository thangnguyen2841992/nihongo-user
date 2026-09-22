package com.thang.nihongo_user.wallet;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.wallet.WalletServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock IUserWalletRepository wallets;
    @Mock IWalletTransactionRepository transactions;
    @Mock IWalletDepositRepository deposits;
    @Mock com.thang.nihongo_user.wallet.events.WalletEventRecorder events;
    @InjectMocks WalletServiceImpl service;
    final String owner = "d0e8ce2c-2279-433c-84b1-3aaed662ed81";
    UserWallet wallet;
    @BeforeEach void setup() {
        wallet = UserWallet.builder().walletId(1L).userId(owner).notificationEmail("owner@example.com").balance(new BigDecimal("5000")).build();
    }
    void lock() { when(wallets.findByUserIdForUpdate(owner)).thenReturn(Optional.of(wallet)); }
    DepositWalletRequest request(String amount) {
        DepositWalletRequest r = new DepositWalletRequest(); r.setAmount(new BigDecimal(amount));
        r.setDescription("test"); r.setRequestKey("4f33f16e-7d57-440d-9738-c6962c47e169"); return r;
    }
    WalletDeposit pending() {
        WalletDeposit d = new WalletDeposit(); d.setId(10L); d.setUserId(owner);
        d.setAmount(new BigDecimal("10000")); d.setDescription("test");
        d.setRequestKey(request("10000").getRequestKey()); return d;
    }
    ReviewDepositRequest review(boolean approve) {
        ReviewDepositRequest r = new ReviewDepositRequest(); r.setApprove(approve);
        r.setBankReference("bank-123"); r.setNote("Đối soát"); return r;
    }
    void reviewing(WalletDeposit d) {
        lock(); when(deposits.findOwnerById(10L)).thenReturn(Optional.of(owner));
        when(deposits.findForUpdate(10L)).thenReturn(Optional.of(d));
    }
    @Test void firstWalletIsEnsuredAndReadUsingAuthenticatedOwner() {
        lock(); assertEquals(owner, service.getWallet(owner, "owner@example.com").getUserId());
        verify(wallets).ensureWallet(owner);
    }
    @Test void requestDoesNotCreditBalance() {
        lock(); when(deposits.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(PaymentStatus.PENDING, service.deposit(owner, "owner@example.com", request("10000")).getStatus());
        assertEquals(new BigDecimal("5000"), wallet.getBalance());
        verifyNoInteractions(transactions);
    }
    @Test void sameKeyReturnsOriginalRequest() {
        lock(); WalletDeposit d = pending();
        when(deposits.findByUserIdAndRequestKey(owner, d.getRequestKey())).thenReturn(Optional.of(d));
        assertSame(d, service.deposit(owner, "owner@example.com", request("10000")));
        verify(deposits, never()).save(any()); verifyNoInteractions(transactions);
    }
    @Test void reusedKeyWithDifferentAmountIsRejected() {
        lock(); WalletDeposit d = pending();
        when(deposits.findByUserIdAndRequestKey(owner, d.getRequestKey())).thenReturn(Optional.of(d));
        assertThrows(ResponseStatusException.class, () -> service.deposit(owner, "owner@example.com", request("20000")));
    }
    @Test void rejectsSmallFractionalAndExcessiveAmounts() {
        for (String amount : List.of("0", "-1000", "999", "1000.5", "100000001"))
            assertThrows(ResponseStatusException.class, () -> service.deposit(owner, "owner@example.com", request(amount)));
        verifyNoInteractions(wallets, transactions, deposits);
    }
    @Test void approvalCreditsExactlyOnceAndWritesLedger() {
        WalletDeposit d = pending(); reviewing(d);
        when(deposits.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.review(10L, "admin", review(true));
        service.review(10L, "admin", review(true));
        assertEquals(new BigDecimal("15000"), wallet.getBalance());
        assertEquals(PaymentStatus.SUCCESS, d.getStatus()); assertEquals("admin", d.getReviewedBy());
        ArgumentCaptor<WalletTransaction> ledger = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactions, times(1)).save(ledger.capture());
        assertEquals(new BigDecimal("5000"), ledger.getValue().getBalanceBefore());
        assertEquals(new BigDecimal("15000"), ledger.getValue().getBalanceAfter());
        assertEquals(10L, ledger.getValue().getReferenceId());
    }
    @Test void duplicateBankReferenceIsRejected() {
        reviewing(pending()); when(deposits.existsByBankReference("BANK-123")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.review(10L, "admin", review(true)));
        verifyNoInteractions(transactions); assertEquals(new BigDecimal("5000"), wallet.getBalance());
    }
    @Test void rejectionNeverCreditsAndCannotLaterBeApproved() {
        WalletDeposit d = pending(); reviewing(d);
        when(deposits.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.review(10L, "admin", review(false));
        assertEquals(PaymentStatus.CANCELLED, d.getStatus());
        assertThrows(ResponseStatusException.class, () -> service.review(10L, "admin", review(true)));
        verifyNoInteractions(transactions);
    }
    @Test void approvalRequiresBankReference() {
        reviewing(pending()); ReviewDepositRequest r = review(true); r.setBankReference("");
        assertThrows(ResponseStatusException.class, () -> service.review(10L, "admin", r));
        verifyNoInteractions(transactions);
    }
    @Test void rejectionRequiresReason() {
        reviewing(pending()); ReviewDepositRequest r = review(false); r.setNote("");
        assertThrows(ResponseStatusException.class, () -> service.review(10L, "admin", r));
        verifyNoInteractions(transactions);
    }
}

