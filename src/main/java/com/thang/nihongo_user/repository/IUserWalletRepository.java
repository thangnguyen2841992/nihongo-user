package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM UserWallet w
        WHERE w.userId = :userId
    """)
    Optional<UserWallet> findByUserIdForUpdate(
            @Param("userId") Long userId
    );
}
