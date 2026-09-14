package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.PaymentMethod;
import com.thang.nihongo_user.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IPaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PaymentTransaction> findByPaymentMethodAndProviderTransactionId(PaymentMethod paymentMethod, String providerTransactionId);
}
