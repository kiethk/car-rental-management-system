package com.crms.mvc.repository;

import com.crms.mvc.entity.Payment;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdOrderByPaidAtAsc(Long bookingId);

    Optional<Payment> findByBookingIdAndPaymentType(Long bookingId, PaymentType paymentType);

    Optional<Payment> findFirstByBookingIdAndPaymentTypeAndStatus(Long bookingId, PaymentType paymentType, PaymentStatus status);

    List<Payment> findByPaymentTypeAndStatus(PaymentType paymentType, PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentType = com.crms.mvc.enums.PaymentType.REFUND AND p.status = com.crms.mvc.enums.PaymentStatus.PENDING")
    long countPendingRefunds();

    @Query("SELECT DISTINCT p.booking.id FROM Payment p WHERE p.paymentType = com.crms.mvc.enums.PaymentType.REFUND AND p.status = com.crms.mvc.enums.PaymentStatus.PENDING")
    List<Long> findBookingIdsWithPendingRefund();
}
