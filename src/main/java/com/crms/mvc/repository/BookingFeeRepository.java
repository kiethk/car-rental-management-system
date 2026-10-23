package com.crms.mvc.repository;

import com.crms.mvc.entity.BookingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingFeeRepository extends JpaRepository<BookingFee, Long> {
    List<BookingFee> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
    List<BookingFee> findByIssueId(Long issueId);
}
