package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.WalletDeposit;
import com.thang.nihongo_user.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface IWalletDepositRepository extends JpaRepository<WalletDeposit, Long> {
    Optional<WalletDeposit> findByUserIdAndRequestKey(String userId, String requestKey);
    List<WalletDeposit> findTop100ByUserIdOrderByCreatedAtDesc(String userId);
    List<WalletDeposit> findTop100ByStatusOrderByCreatedAtAsc(PaymentStatus status);
    List<WalletDeposit> findTop100ByStatusNotOrderByCreatedAtDesc(PaymentStatus status);
    @Query("select d.userId from WalletDeposit d where d.id = :id")
    Optional<String> findOwnerById(@Param("id") Long id);
    boolean existsByBankReference(String bankReference);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from WalletDeposit d where d.id = :id")
    Optional<WalletDeposit> findForUpdate(@Param("id") Long id);
}
