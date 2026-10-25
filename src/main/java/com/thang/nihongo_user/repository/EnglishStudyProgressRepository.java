package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.EnglishStudyProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EnglishStudyProgressRepository extends JpaRepository<EnglishStudyProgress, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EnglishStudyProgress p where p.userId = :userId")
    Optional<EnglishStudyProgress> lock(@Param("userId") String userId);
}
