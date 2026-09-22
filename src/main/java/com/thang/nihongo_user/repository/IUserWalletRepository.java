package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(String userId);

    // The unique user_id serializes concurrent creation of the first wallet.
    @Modifying
    @Query(value = "INSERT INTO user_wallet (user_id, balance, created_at, updated_at) VALUES (:userId, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE user_id = user_id", nativeQuery = true)
    void ensureWallet(@Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM UserWallet w
        WHERE w.userId = :userId
    """)
    Optional<UserWallet> findByUserIdForUpdate(
            @Param("userId") String userId
    );
}
