package com.crms.mvc.repository;

import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingFeeRepository extends JpaRepository<BookingFee, Long> {
    List<BookingFee> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
    List<BookingFee> findByIssueId(Long issueId);
    List<BookingFee> findByIssueIdIn(Collection<Long> issueIds);

    List<BookingFee> findByPaymentId(Long paymentId);

    /** Issue fees of resolved issues that no paid payment has taken yet: what customers still owe after settlement. */
    @Query("SELECT COUNT(f) FROM BookingFee f JOIN f.issue i JOIN f.booking b LEFT JOIN f.payment p "
            + "WHERE f.feeType = 'ISSUE_COMPENSATION' AND i.status = :resolved AND b.status = 'completed' AND (p IS NULL OR p.status <> 'paid')")
    long countIssueFeesAwaitingPayment(@Param("resolved") IssueStatus resolved);

    /** Issue fees taken by a payment that was paid inside the period. */
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM BookingFee f JOIN f.payment p "
            + "WHERE f.feeType = 'ISSUE_COMPENSATION' AND p.status = 'paid' AND p.paidAt >= :from AND p.paidAt < :to")
    BigDecimal sumIssueFeesPaidBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
