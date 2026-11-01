package com.crms.mvc.service;

import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.IssueCancelledEvent;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueEvidenceRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.impl.IssueServiceImpl;
import com.crms.mvc.util.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The life of an Issue: open -> resolved (via direct counter settlement, checkout, or online payment)
 * or cancelled. Once resolved, the issue is locked immutable.
 */
class IssueLifecycleTest {
    private IssueRepository issueRepository;
    private BookingFeeRepository bookingFeeRepository;
    private PaymentRepository paymentRepository;
    private IssueService service;
    private ApplicationEventPublisher eventPublisher;

    private final IssueActor staff = new IssueActor(2L, EmployeeRole.STAFF);
    private final IssueActor admin = new IssueActor(1L, EmployeeRole.ADMIN);

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        bookingFeeRepository = mock(BookingFeeRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new IssueServiceImpl(issueRepository, mock(BookingRepository.class), mock(EmployeeRepository.class),
                bookingFeeRepository, mock(IssueEvidenceRepository.class), mock(FileUploadService.class),
                mock(LicensePlateRepository.class), paymentRepository, eventPublisher);
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingFeeRepository.save(any(BookingFee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- direct counter settlement ----

    @Test
    void directCounterSettlementResolvesIssueAndCreatesPaidPayment() {
        Issue issue = issue(IssueStatus.OPEN);
        BookingFee fee = feeOf(issue, null);

        Issue resolved = service.settleDirectly(7L, "CASH", "Khách nộp tiền mặt", staff);

        assertEquals(IssueStatus.RESOLVED, resolved.getStatus());
        assertEquals("Khách nộp tiền mặt", resolved.getResolutionNote());
        assertNotNull(resolved.getResolvedAt());
        assertNotNull(fee.getPayment());
        assertEquals(Payment.STATUS_PAID, fee.getPayment().getStatus());
        assertEquals("CASH", fee.getPayment().getPaymentMethod());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void cannotSettleDirectlyWhenIssueIsNotOpen() {
        for (IssueStatus status : List.of(IssueStatus.RESOLVED, IssueStatus.CANCELLED)) {
            issue(status);
            assertThrows(IllegalStateException.class, () -> service.settleDirectly(7L, "CASH", "note", staff));
        }
    }

    // ---- cancel ----

    @Test
    void staffCanCancelAnOpenIssueAndItsFeeDisappearsAndEventIsPublished() {
        Issue issue = issue(IssueStatus.OPEN);
        BookingFee fee = feeOf(issue, null);

        service.cancel(7L, "Recorded on the wrong booking", staff);

        assertEquals(IssueStatus.CANCELLED, issue.getStatus());
        assertEquals("Recorded on the wrong booking", issue.getCancelReason());
        verify(bookingFeeRepository).delete(fee);
        verify(eventPublisher).publishEvent(new IssueCancelledEvent(7L, "Recorded on the wrong booking"));
    }

    @Test
    void cancellingNeedsAReason() {
        Issue issue = issue(IssueStatus.OPEN);

        assertThrows(IllegalArgumentException.class, () -> service.cancel(7L, " ", staff));

        assertEquals(IssueStatus.OPEN, issue.getStatus());
    }

    @Test
    void resolvedIssueCannotBeCancelled() {
        Issue issue = issue(IssueStatus.RESOLVED);
        BookingFee fee = feeOf(issue, null);

        assertThrows(IllegalStateException.class, () -> service.cancel(7L, "complaint", admin));

        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
        verify(bookingFeeRepository, never()).delete(fee);
    }

    @Test
    void aCancelledIssueCannotBeCancelledAgain() {
        issue(IssueStatus.CANCELLED);

        assertThrows(IllegalStateException.class, () -> service.cancel(7L, "again", admin));
    }

    // ---- reopen is disabled ----

    @Test
    void reopeningIsDisabledOnceResolvedOrCancelled() {
        for (IssueStatus status : List.of(IssueStatus.RESOLVED, IssueStatus.CANCELLED, IssueStatus.OPEN)) {
            issue(status);
            assertThrows(IllegalStateException.class, () -> service.reopen(7L, admin));
        }
    }

    private Issue issue(IssueStatus status) {
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setStatus(Booking.STATUS_ACTIVE);
        Issue issue = new Issue();
        issue.setId(7L);
        issue.setBooking(booking);
        issue.setIssueType(IssueType.DAMAGE);
        issue.setStatus(status);
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        return issue;
    }

    private BookingFee feeOf(Issue issue, Payment payment) {
        BookingFee fee = new BookingFee();
        fee.setId(70L);
        fee.setBooking(issue.getBooking());
        fee.setIssue(issue);
        fee.setFeeType(BookingFee.FEE_TYPE_ISSUE_COMPENSATION);
        fee.setAmount(new BigDecimal("200000"));
        fee.setPayment(payment);
        when(bookingFeeRepository.findByIssueId(issue.getId())).thenReturn(List.of(fee));
        return fee;
    }
}
