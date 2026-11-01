package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.service.BookingFeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
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
    public List<BookingFee> getSettleableIssueFees(Long bookingId) {
        return bookingFeeRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .filter(fee -> BookingFee.FEE_TYPE_ISSUE_COMPENSATION.equals(fee.getFeeType()))
                .filter(fee -> fee.getPayment() == null || !Payment.STATUS_PAID.equalsIgnoreCase(fee.getPayment().getStatus()))
                .filter(fee -> fee.getIssue() != null && fee.getIssue().getStatus() == IssueStatus.OPEN)
                .toList();
    }

    @Override
    public void attachPayment(Collection<BookingFee> fees, Payment payment) {
        fees.forEach(fee -> {
            fee.setPayment(payment);
            if (Payment.STATUS_PAID.equalsIgnoreCase(payment.getStatus()) && fee.getIssue() != null) {
                Issue issue = fee.getIssue();
                if (issue.getStatus() == IssueStatus.OPEN) {
                    issue.setStatus(IssueStatus.RESOLVED);
                    issue.setResolutionNote(Payment.TYPE_FINAL.equalsIgnoreCase(payment.getPaymentType())
                            || Payment.TYPE_REFUND.equalsIgnoreCase(payment.getPaymentType())
                            ? "Đã cấn trừ vào tiền cọc khi quyết toán trả xe"
                            : "Đã thanh toán (" + payment.getPaymentMethod() + ")");
                    issue.setResolvedAt(OffsetDateTime.now());
                    issueRepository.save(issue);
                }
            }
        });
        bookingFeeRepository.saveAll(fees);
    }

    @Override
    public void confirmPayment(Payment payment) {
        List<BookingFee> taken = bookingFeeRepository.findByPaymentId(payment.getId());
        attachPayment(taken, payment);
    }

    @Override
    public void detachPayment(Payment payment) {
        List<BookingFee> taken = bookingFeeRepository.findByPaymentId(payment.getId());
        taken.forEach(fee -> fee.setPayment(null));
        bookingFeeRepository.saveAll(taken);
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
