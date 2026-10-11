package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.PromotionRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption,Long> {
 boolean existsByPromotionIdAndUserId(Long promotionId,String userId);
}
