package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.CoursePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CoursePurchaseRepository extends JpaRepository<CoursePurchase,Long> {
 Optional<CoursePurchase> findByUserIdAndRequestKey(String userId,String requestKey);
}
