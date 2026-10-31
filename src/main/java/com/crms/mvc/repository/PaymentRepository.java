package com.crms.mvc.repository;

import com.crms.mvc.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdOrderByPaidAtAsc(Long bookingId);

    Optional<Payment> findByBookingIdAndPaymentType(Long bookingId, String paymentType);

    Optional<Payment> findFirstByBookingIdAndPaymentTypeAndStatus(Long bookingId, String paymentType, String status);

    List<Payment> findByPaymentTypeAndStatus(String paymentType, String status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentType = 'REFUND' AND p.status = 'pending'")
    long countPendingRefunds();

    @Query("SELECT DISTINCT p.booking.id FROM Payment p WHERE p.paymentType = 'REFUND' AND p.status = 'pending'")
    List<Long> findBookingIdsWithPendingRefund();
}
