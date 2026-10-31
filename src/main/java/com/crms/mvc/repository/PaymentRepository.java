package com.crms.mvc.repository;

import com.crms.mvc.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdOrderByPaidAtAsc(Long bookingId);

    Optional<Payment> findByBookingIdAndPaymentType(Long bookingId, String paymentType);
}
