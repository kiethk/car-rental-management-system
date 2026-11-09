package com.crms.mvc.service;

import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.service.impl.BookingFeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Only fees of open, unpaid issues are ready to collect at settlement; resolved or cancelled ones do not apply. */
class BookingFeeServiceTest {
    private BookingFeeRepository feeRepository;
    private BookingFeeService service;

    @BeforeEach
    void setUp() {
        feeRepository = mock(BookingFeeRepository.class);
        service = new BookingFeeServiceImpl(feeRepository, mock(BookingRepository.class), mock(IssueRepository.class));
    }

    @Test
    void onlyOpenUnpaidIssueFeesAreSettleable() {
        BookingFee ready = issueFee(IssueStatus.OPEN, null);
        BookingFee resolved = issueFee(IssueStatus.RESOLVED, null);
        BookingFee cancelled = issueFee(IssueStatus.CANCELLED, null);
        Payment paid = new Payment();
        paid.setStatus(Payment.STATUS_PAID);
        BookingFee alreadyPaid = issueFee(IssueStatus.OPEN, paid);
        BookingFee lateFee = plainFee("LATE_FEE");
        when(feeRepository.findByBookingIdOrderByCreatedAtAsc(42L))
                .thenReturn(List.of(ready, resolved, cancelled, alreadyPaid, lateFee));

        List<BookingFee> settleable = service.getSettleableIssueFees(42L);

        assertEquals(List.of(ready), settleable);
    }

    @Test
    void attachingAPaymentMarksEveryFeeAsTaken() {
        BookingFee first = issueFee(IssueStatus.RESOLVED, null);
        BookingFee second = plainFee("LATE_FEE");
        Payment payment = new Payment();

        service.attachPayment(List.of(first, second), payment);

        assertSame(payment, first.getPayment());
        assertSame(payment, second.getPayment());
        verify(feeRepository).saveAll(List.of(first, second));
    }

    @Test
    void detachingAPaymentGivesItsFeesBackAsOwed() {
        Payment payment = new Payment();
        payment.setId(77L);
        BookingFee first = issueFee(IssueStatus.RESOLVED, payment);
        BookingFee second = issueFee(IssueStatus.RESOLVED, payment);
        when(feeRepository.findByPaymentId(77L)).thenReturn(List.of(first, second));

        service.detachPayment(payment);

        assertNull(first.getPayment());
        assertNull(second.getPayment());
        verify(feeRepository).saveAll(List.of(first, second));
    }

    private BookingFee issueFee(IssueStatus issueStatus, Payment payment) {
        Issue issue = new Issue();
        issue.setStatus(issueStatus);
        BookingFee fee = new BookingFee();
        fee.setFeeType(BookingFee.FEE_TYPE_ISSUE_COMPENSATION);
        fee.setIssue(issue);
        fee.setAmount(new BigDecimal("200000"));
        fee.setPayment(payment);
        return fee;
    }

    private BookingFee plainFee(String type) {
        BookingFee fee = new BookingFee();
        fee.setFeeType(type);
        fee.setAmount(new BigDecimal("50000"));
        return fee;
    }
}
