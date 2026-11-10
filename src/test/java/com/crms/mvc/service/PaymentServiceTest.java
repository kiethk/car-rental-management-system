package com.crms.mvc.service;

import com.crms.mvc.service.impl.VnPayGateway;
import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.FeeType;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.PaymentReceivedEvent;
import com.crms.mvc.event.RefundCompletedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    private PaymentService paymentService;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private ApplicationEventPublisher eventPublisher;
    private VnPayConfig vnPayConfig;
    private BookingFeeService bookingFeeService;

    @BeforeEach
    public void setUp() {
        vnPayConfig = new VnPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "tmnCode", "CGXZLS0Z");
        ReflectionTestUtils.setField(vnPayConfig, "hashSecret", "XNBCJFAKAZQSGZSSTGIRQUXNMZFVOQD8");
        ReflectionTestUtils.setField(vnPayConfig, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayConfig, "returnUrl", "http://localhost:8081/payment/vnpay/return");
        ReflectionTestUtils.setField(vnPayConfig, "version", "2.1.0");
        ReflectionTestUtils.setField(vnPayConfig, "command", "pay");

        paymentRepository = Mockito.mock(PaymentRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        bookingFeeService = Mockito.mock(BookingFeeService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        paymentService = new PaymentServiceImpl(paymentRepository, bookingRepository, bookingFeeService, eventPublisher, new VnPayGateway(vnPayConfig));
    }

    @Test
    public void testCreateDepositPaymentUrl() {
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setDepositAmount(new BigDecimal("675000"));

        Mockito.when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        String url = paymentService.createDepositPaymentUrl(7L, "127.0.0.1");

        assertNotNull(url);
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_Amount=67500000"));
        assertTrue(url.contains("vnp_TmnCode=CGXZLS0Z"));
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    public void testCreatePendingRefund() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setDepositAmount(new BigDecimal("500000"));

        Mockito.when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment refund = paymentService.createPendingRefund(10L, new BigDecimal("500000"), "VCB", "1234567890", "NGUYEN VAN A");

        assertNotNull(refund);
        assertEquals(PaymentType.REFUND, refund.getPaymentType());
        assertEquals(PaymentStatus.PENDING, refund.getStatus());
        assertEquals(new BigDecimal("500000"), refund.getAmount());
        assertEquals("VCB: 1234567890 - NGUYEN VAN A", refund.getTransactionCode());
    }

    @Test
    public void testConfirmRefundCompleted() {
        Booking booking = new Booking();
        booking.setId(10L);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setBooking(booking);
        payment.setPaymentType(PaymentType.REFUND);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionCode("VCB: 1234567890 - NGUYEN VAN A");

        Mockito.when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment confirmed = paymentService.confirmRefundCompleted(100L, 1L, null);

        assertNotNull(confirmed);
        assertEquals(PaymentStatus.PAID, confirmed.getStatus());
        assertNotNull(confirmed.getPaidAt());
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RefundCompletedEvent.class));
    }

    @Test
    public void testALiveIssuePaymentSessionIsReported() {
        Payment pending = new Payment();
        pending.setStatus(PaymentStatus.PENDING);
        pending.setTransactionCode("ISSUE-11-" + System.currentTimeMillis());
        Mockito.when(paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                11L, PaymentType.ISSUE_COMPENSATION, PaymentStatus.PENDING)).thenReturn(Optional.of(pending));

        assertTrue(paymentService.hasLiveIssuePaymentSession(11L));
        assertEquals(PaymentStatus.PENDING, pending.getStatus());
    }

    @Test
    public void testAnExpiredIssuePaymentSessionIsReleasedAndDoesNotBlock() {
        Payment pending = new Payment();
        pending.setStatus(PaymentStatus.PENDING);
        pending.setTransactionCode("ISSUE-11-" + (System.currentTimeMillis() - 16 * 60 * 1000L));
        Mockito.when(paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                11L, PaymentType.ISSUE_COMPENSATION, PaymentStatus.PENDING)).thenReturn(Optional.of(pending));

        assertFalse(paymentService.hasLiveIssuePaymentSession(11L));
        assertEquals(PaymentStatus.FAILED, pending.getStatus());
        Mockito.verify(bookingFeeService).detachPayment(pending);
    }

    // ---- paying issue fees after settlement ----

    @Test
    public void testIssuePaymentChargesExactlyTheOutstandingFeesAndTakesThem() {
        Booking booking = openRentalOf(5L);
        BookingFee first = fee(FeeType.ISSUE_COMPENSATION, "200000");
        BookingFee second = fee(FeeType.ISSUE_COMPENSATION, "500000");
        Mockito.when(bookingFeeService.getSettleableIssueFees(42L)).thenReturn(List.of(first, second));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });

        String url = paymentService.createIssuePaymentUrl(42L, 5L, "203.0.113.7");

        org.mockito.ArgumentCaptor<Payment> captor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        Mockito.verify(paymentRepository).save(captor.capture());
        Payment pending = captor.getValue();
        assertEquals(PaymentType.ISSUE_COMPENSATION, pending.getPaymentType());
        assertEquals(PaymentStatus.PENDING, pending.getStatus());
        assertEquals(new BigDecimal("700000"), pending.getAmount());
        assertSame(booking, pending.getBooking());
        Mockito.verify(bookingFeeService).attachPayment(List.of(first, second), pending);
        // VNPay counts in 1/100 of a dong; the reference carries the payment so the callback can find it
        assertTrue(url.contains("vnp_Amount=70000000"), url);
        assertTrue(url.contains("vnp_TxnRef=I77_"), url);
    }

    @Test
    public void testIssuePaymentIsOnlyForTheBookingsOwner() {
        openRentalOf(5L);

        assertThrows(java.util.NoSuchElementException.class, () -> paymentService.createIssuePaymentUrl(42L, 6L, "127.0.0.1"));
        assertThrows(java.util.NoSuchElementException.class, () -> paymentService.createIssuePaymentUrl(42L, null, "127.0.0.1"));

        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testIssuePaymentNeedsAnOpenRentalAndSomethingOwed() {
        Booking booking = openRentalOf(5L);
        Mockito.when(bookingFeeService.getSettleableIssueFees(42L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> paymentService.createIssuePaymentUrl(42L, 5L, "127.0.0.1"));

        // a settled booking is closed: its Issue fees were collected by the settlement
        booking.setStatus(BookingStatus.COMPLETED);
        assertThrows(IllegalStateException.class, () -> paymentService.createIssuePaymentUrl(42L, 5L, "127.0.0.1"));

        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testAnAbandonedPendingIssuePaymentIsCancelledBeforeANewOneStarts() {
        openRentalOf(5L);
        Payment stale = new Payment();
        stale.setId(70L);
        stale.setPaymentType(PaymentType.ISSUE_COMPENSATION);
        stale.setStatus(PaymentStatus.PENDING);
        Mockito.when(paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(42L, PaymentType.ISSUE_COMPENSATION, PaymentStatus.PENDING))
                .thenReturn(Optional.of(stale));
        Mockito.when(bookingFeeService.getSettleableIssueFees(42L)).thenReturn(List.of(fee(FeeType.ISSUE_COMPENSATION, "200000")));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createIssuePaymentUrl(42L, 5L, "127.0.0.1");

        assertEquals(PaymentStatus.FAILED, stale.getStatus());
        Mockito.verify(bookingFeeService).detachPayment(stale);
    }

    @Test
    public void testASuccessfulIssuePaymentIsMarkedPaidAndRepeatingItChangesNothing() {
        Payment pending = pendingIssuePayment(77L, "700000");

        Payment paid = paymentService.confirmIssuePayment(77L, "VNP123", 70000000L);

        assertEquals(PaymentStatus.PAID, paid.getStatus());
        assertEquals("VNP123", paid.getTransactionCode());
        assertNotNull(paid.getPaidAt());

        Payment again = paymentService.confirmIssuePayment(77L, "VNP999", 70000000L);
        assertSame(pending, again);
        assertEquals("VNP123", again.getTransactionCode());

        // The gateway may call back twice, but the customer is thanked once.
        org.mockito.ArgumentCaptor<PaymentReceivedEvent> thanks = org.mockito.ArgumentCaptor.forClass(PaymentReceivedEvent.class);
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(thanks.capture());
        assertEquals(PaymentReceivedEvent.Kind.ISSUE_FEES, thanks.getValue().kind());
        assertEquals(42L, thanks.getValue().bookingId());
        assertEquals(new BigDecimal("700000"), thanks.getValue().amount());
    }

    @Test
    public void testTheRemainingRentIsAcknowledgedWithAThankYou() {
        openRentalOf(5L);
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.recordRentalBalancePayment(42L, new BigDecimal("500000"), PaymentMethod.CASH);

        org.mockito.ArgumentCaptor<PaymentReceivedEvent> thanks = org.mockito.ArgumentCaptor.forClass(PaymentReceivedEvent.class);
        Mockito.verify(eventPublisher).publishEvent(thanks.capture());
        assertEquals(PaymentReceivedEvent.Kind.RENTAL_BALANCE, thanks.getValue().kind());
        assertEquals("CASH", thanks.getValue().paymentMethod());
    }

    @Test
    public void testTheSettlementPaymentIsAcknowledgedWithAThankYou() {
        openRentalOf(5L);
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.recordFinalPayment(42L, new BigDecimal("300000"), PaymentMethod.BANK_TRANSFER);

        org.mockito.ArgumentCaptor<PaymentReceivedEvent> thanks = org.mockito.ArgumentCaptor.forClass(PaymentReceivedEvent.class);
        Mockito.verify(eventPublisher).publishEvent(thanks.capture());
        assertEquals(PaymentReceivedEvent.Kind.SETTLEMENT, thanks.getValue().kind());
    }

    @Test
    public void testAnIssuePaymentWithTheWrongAmountIsRefusedAndStaysPending() {
        Payment pending = pendingIssuePayment(77L, "700000");

        assertThrows(IllegalArgumentException.class, () -> paymentService.confirmIssuePayment(77L, "VNP123", 100L));

        assertEquals(PaymentStatus.PENDING, pending.getStatus());
    }

    @Test
    public void testAnIssuePaymentThatAlreadyExpiredCannotBeConfirmed() {
        Payment expired = pendingIssuePayment(77L, "700000");
        expired.setStatus(PaymentStatus.FAILED);

        assertThrows(IllegalStateException.class, () -> paymentService.confirmIssuePayment(77L, "VNP123", 70000000L));
    }

    @Test
    public void testAnIssuePaymentMarkedFailedCanBeRecoveredIfFeesStillOwed() {
        Booking booking = openRentalOf(5L);
        Payment payment = pendingIssuePayment(77L, "700000");
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.FAILED);

        BookingFee fee = fee(FeeType.ISSUE_COMPENSATION, "700000");
        Mockito.when(bookingFeeService.getSettleableIssueFees(42L)).thenReturn(List.of(fee));

        Payment paid = paymentService.confirmIssuePayment(77L, "VNP123", 70000000L);

        assertEquals(PaymentStatus.PAID, paid.getStatus());
        assertEquals("VNP123", paid.getTransactionCode());
        Mockito.verify(bookingFeeService).attachPayment(List.of(fee), payment);
    }

    @Test
    public void testAFailedIssuePaymentGivesItsFeesBackAsOwed() {
        Payment pending = pendingIssuePayment(77L, "700000");

        Payment failed = paymentService.failIssuePayment(77L);

        assertEquals(PaymentStatus.FAILED, failed.getStatus());
        Mockito.verify(bookingFeeService).detachPayment(pending);
    }

    @Test
    public void testAFailureReportedForAnAlreadyPaidIssuePaymentIsIgnored() {
        Payment paid = pendingIssuePayment(77L, "700000");
        paid.setStatus(PaymentStatus.PAID);

        Payment result = paymentService.failIssuePayment(77L);

        assertEquals(PaymentStatus.PAID, result.getStatus());
        Mockito.verify(bookingFeeService, Mockito.never()).detachPayment(Mockito.any());
    }

    private Booking openRentalOf(Long customerId) {
        com.crms.mvc.entity.Customer customer = new com.crms.mvc.entity.Customer();
        customer.setId(customerId);
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.ACTIVE);
        Mockito.when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        return booking;
    }

    private Payment pendingIssuePayment(Long id, String amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setPaymentType(PaymentType.ISSUE_COMPENSATION);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal(amount));
        payment.setBooking(openRentalOf(5L));
        Mockito.when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return payment;
    }

    private BookingFee fee(FeeType type, String amount) {
        BookingFee fee = new BookingFee();
        fee.setFeeType(type);
        fee.setAmount(new BigDecimal(amount));
        return fee;
    }
}
