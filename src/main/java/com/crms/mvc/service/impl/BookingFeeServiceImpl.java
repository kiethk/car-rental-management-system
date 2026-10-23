package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.service.BookingFeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class BookingFeeServiceImpl implements BookingFeeService {
    private final BookingFeeRepository bookingFeeRepository;
    private final BookingRepository bookingRepository;
    private final IssueRepository issueRepository;

    public BookingFeeServiceImpl(BookingFeeRepository bookingFeeRepository,
                                 BookingRepository bookingRepository,
                                 IssueRepository issueRepository) {
        this.bookingFeeRepository = bookingFeeRepository;
        this.bookingRepository = bookingRepository;
        this.issueRepository = issueRepository;
    }

    @Override
    public BookingFee recordFee(Long bookingId, String feeType, BigDecimal amount, String description, Long issueId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        Issue issue = null;
        if (issueId != null) {
            issue = issueRepository.findById(issueId).orElse(null);
        }

        BookingFee fee = new BookingFee();
        fee.setBooking(booking);
        fee.setFeeType(feeType);
        fee.setAmount(amount != null ? amount : BigDecimal.ZERO);
        fee.setDescription(description != null ? description : feeType);
        fee.setIssue(issue);
        fee.setCreatedAt(OffsetDateTime.now());

        return bookingFeeRepository.save(fee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingFee> getFeesByBooking(Long bookingId) {
        return bookingFeeRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAdditionalFees(Long bookingId) {
        return bookingFeeRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)
                .stream()
                .map(BookingFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
