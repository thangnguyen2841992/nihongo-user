package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.Promotion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface PromotionRepository extends JpaRepository<Promotion,Long> {
 Optional<Promotion> findByCode(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Promotion p where p.code = :code")
 Optional<Promotion> lockByCode(String code);
 Page<Promotion> findAllByOrderByIdDesc(Pageable page);
}
