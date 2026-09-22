package com.thang.nihongo_user.wallet;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.wallet.WalletServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:wallet_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false", "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = WalletPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WalletPersistenceTest {
    @TestConfiguration
    @EntityScan(basePackageClasses = UserWallet.class)
    @EnableJpaRepositories(basePackageClasses = IUserWalletRepository.class)
    @Import({WalletServiceImpl.class, com.thang.nihongo_user.wallet.events.WalletEventRecorder.class, JsonConfig.class})
    static class Config {}
    @TestConfiguration static class JsonConfig { @org.springframework.context.annotation.Bean com.fasterxml.jackson.databind.ObjectMapper mapper() { return new com.fasterxml.jackson.databind.ObjectMapper(); } }
    @Autowired WalletServiceImpl service;
    @Autowired IUserWalletRepository wallets;
    @Autowired IWalletDepositRepository deposits;
    @Autowired IWalletTransactionRepository ledger;
    @BeforeEach void clear() { ledger.deleteAll(); deposits.deleteAll(); wallets.deleteAll(); }
    DepositWalletRequest request() {
        DepositWalletRequest r = new DepositWalletRequest(); r.setAmount(new BigDecimal("10000"));
        r.setRequestKey(UUID.randomUUID().toString()); return r;
    }
    ReviewDepositRequest approval() {
        ReviewDepositRequest r = new ReviewDepositRequest(); r.setApprove(true); r.setBankReference("BANK-UNIQUE"); return r;
    }
    List<Object> concurrently(Callable<Object> a, Callable<Object> b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : List.of(a, b)) futures.add(executor.submit(() -> {
                start.await(); try { return task.call(); } catch (Exception e) { return e; }
            }));
            start.countDown();
            List<Object> result = new ArrayList<>();
            for (Future<Object> f : futures) result.add(f.get(20, TimeUnit.SECONDS));
            return result;
        } finally { executor.shutdownNow(); }
    }
    @Test void concurrentFirstRequestCreatesOneWalletAndOneDeposit() throws Exception {
        DepositWalletRequest r = request();
        List<Object> result = concurrently(() -> service.deposit("owner", "owner@example.com", r), () -> service.deposit("owner", "owner@example.com", r));
        assertTrue(result.stream().allMatch(WalletDeposit.class::isInstance), result.toString());
        assertEquals(1, wallets.count()); assertEquals(1, deposits.count()); assertEquals(0, ledger.count());
        assertEquals(0, service.getWallet("owner", "owner@example.com").getBalance().compareTo(BigDecimal.ZERO));
    }
    @Test void simultaneousApprovalCreditsOnlyOnce() throws Exception {
        Long id = service.deposit("owner", "owner@example.com", request()).getId();
        List<Object> result = concurrently(() -> service.review(id, "admin1", approval()), () -> service.review(id, "admin2", approval()));
        assertTrue(result.stream().allMatch(WalletDeposit.class::isInstance), result.toString());
        assertEquals(1, ledger.count());
        assertEquals(0, service.getWallet("owner", "owner@example.com").getBalance().compareTo(new BigDecimal("10000")));
    }
    @Test void duplicateBankReferenceAcrossWalletsRollsBackLosingCredit() throws Exception {
        Long a = service.deposit("owner-a", "owner@example.com", request()).getId();
        Long b = service.deposit("owner-b", "owner@example.com", request()).getId();
        List<Object> result = concurrently(() -> service.review(a, "admin", approval()), () -> service.review(b, "admin", approval()));
        assertEquals(1, result.stream().filter(WalletDeposit.class::isInstance).count(), result.toString());
        assertEquals(1, ledger.count());
        BigDecimal total = service.getWallet("owner-a", "owner@example.com").getBalance().add(service.getWallet("owner-b", "owner@example.com").getBalance());
        assertEquals(0, total.compareTo(new BigDecimal("10000")));
        assertEquals(1, deposits.findAll().stream().filter(d -> d.getStatus() == PaymentStatus.SUCCESS).count());
    }
}

