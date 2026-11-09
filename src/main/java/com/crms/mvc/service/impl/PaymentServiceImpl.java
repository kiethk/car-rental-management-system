package com.crms.mvc.service.impl;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import com.crms.mvc.dto.DepositSessionDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.DepositConfirmedEvent;
import com.crms.mvc.event.PaymentReceivedEvent;
import com.crms.mvc.event.RefundCompletedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingFeeService bookingFeeService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentGateway gateway;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingFeeService bookingFeeService,
            ApplicationEventPublisher eventPublisher,
            PaymentGateway gateway) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingFeeService = bookingFeeService;
        this.eventPublisher = eventPublisher;
        this.gateway = gateway;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepositSessionDTO> findDepositSession(Long bookingId) {
        return bookingRepository.findById(bookingId).map(booking -> new DepositSessionDTO(booking.getId(),
                booking.getCustomer() != null ? booking.getCustomer().getId() : null,
                EnumTokens.of(booking.getStatus()), booking.getDepositAmount(), booking.getCreatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public String createDepositPaymentUrl(Long bookingId, String clientIp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        return gateway.createPaymentUrl(booking.getDepositAmount(), booking.getId() + "_" + System.currentTimeMillis(),
                "DriveNow_Deposit_Booking_" + booking.getId(), clientIp);
    }

    @Override
    public Payment confirmDeposit(Long bookingId, String transactionNo, PaymentMethod paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return paymentRepository.findByBookingIdAndPaymentType(bookingId, PaymentType.DEPOSIT).orElse(null);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentType(PaymentType.DEPOSIT);
        payment.setTransactionCode(
                transactionNo != null ? transactionNo : "TXN-DEP-" + bookingId + "-" + System.currentTimeMillis());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.VNPAY);
        payment.setAmount(booking.getDepositAmount());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        eventPublisher.publishEvent(new DepositConfirmedEvent(bookingId));

        return payment;
    }

    @Override
    public boolean hasLiveIssuePaymentSession(Long bookingId) {
        Optional<Payment> pending = paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                bookingId, PaymentType.ISSUE_COMPENSATION, PaymentStatus.PENDING);
        if (pending.isEmpty()) {
            return false;
        }
        if (isPaymentExpired(pending.get())) {
            cancelPending(pending.get());
            return false;
        }
        return true;
    }

    @Override
    public boolean isValidGatewayReturn(Map<String, String> queryParams) {
        return gateway.verifySignature(queryParams);
    }

    @Override
    public String createIssuePaymentUrl(Long bookingId, Long customerId, String clientIp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        if (customerId == null || !customerId.equals(booking.getCustomer().getId())) {
            throw new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ thanh toán được phí sự cố khi đơn đang thuê");
        }

        // Kiểm tra lượt thanh toán pending trước đó
        Optional<Payment> existingPending = paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                bookingId, PaymentType.ISSUE_COMPENSATION, PaymentStatus.PENDING);

        if (existingPending.isPresent()) {
            Payment prev = existingPending.get();
            if (!isPaymentExpired(prev)) {
                // Phiên cũ vẫn còn hiệu lực (< 15 phút), tái sử dụng để khách tiếp tục thanh toán
                return gateway.createPaymentUrl(prev.getAmount(), Payment.ISSUE_TXN_PREFIX + prev.getId() + "_" + System.currentTimeMillis(),
                        "DriveNow_Issue_Fees_Booking_" + bookingId, clientIp);
            } else {
                // Phiên cũ đã hết hạn, huỷ để giải phóng phí
                cancelPending(prev);
            }
        }

        List<BookingFee> owed = bookingFeeService.getSettleableIssueFees(bookingId);
        if (owed.isEmpty()) {
            throw new IllegalStateException("Không có khoản phí phát sinh nào cần thanh toán");
        }
        BigDecimal total = owed.stream().map(BookingFee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Payment pending = new Payment();
        pending.setBooking(booking);
        pending.setPaymentType(PaymentType.ISSUE_COMPENSATION);
        pending.setAmount(total);
        pending.setPaymentMethod(PaymentMethod.VNPAY);
        pending.setStatus(PaymentStatus.PENDING);
        pending.setTransactionCode("ISSUE-" + bookingId + "-" + System.currentTimeMillis());
        Payment saved = paymentRepository.save(pending);
        bookingFeeService.attachPayment(owed, saved);

        return gateway.createPaymentUrl(total, Payment.ISSUE_TXN_PREFIX + saved.getId() + "_" + System.currentTimeMillis(),
                "DriveNow_Issue_Fees_Booking_" + bookingId, clientIp);
    }

    @Override
    public Payment confirmIssuePayment(Long paymentId, String transactionNo, long amountInCents) {
        Payment payment = findIssuePayment(paymentId);
        if (PaymentStatus.PAID == payment.getStatus()) {
            return payment; // Cổng có thể gọi callback nhiều lần
        }
        if (PaymentStatus.PENDING != payment.getStatus()) {
            // Cứu giao dịch: Nếu khách đã thanh toán trên tab cũ bị đánh dấu failed nhưng phí vẫn chưa được thanh toán
            Long bookingId = payment.getBooking() != null ? payment.getBooking().getId() : null;
            List<BookingFee> owed = bookingId != null ? bookingFeeService.getSettleableIssueFees(bookingId) : List.of();
            if (!owed.isEmpty()) {
                bookingFeeService.attachPayment(owed, payment);
            } else {
                throw new IllegalStateException("Phiên thanh toán này đã hết hạn; vui lòng thực hiện lại giao dịch mới");
            }
        }
        if (payment.getAmount().multiply(new BigDecimal(100)).longValue() != amountInCents) {
            throw new IllegalArgumentException("Số tiền thanh toán không khớp");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionCode(transactionNo.trim());
        }
        Payment saved = paymentRepository.save(payment);
        bookingFeeService.confirmPayment(saved);
        eventPublisher.publishEvent(PaymentReceivedEvent.of(saved.getBooking(), saved, PaymentReceivedEvent.Kind.ISSUE_FEES));
        return saved;
    }

    @Override
    public Payment failIssuePayment(Long paymentId) {
        Payment payment = findIssuePayment(paymentId);
        if (PaymentStatus.PENDING == payment.getStatus()) {
            cancelPending(payment);
        }
        return payment;
    }

    private Payment findIssuePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khoản thanh toán: " + paymentId));
        if (PaymentType.ISSUE_COMPENSATION != payment.getPaymentType()) {
            throw new IllegalStateException("Giao dịch này không phải là thanh toán phí sự cố");
        }
        return payment;
    }

    private void cancelPending(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        bookingFeeService.detachPayment(payment);
    }

    private boolean isPaymentExpired(Payment payment) {
        if (payment == null) {
            return false;
        }
        String code = payment.getTransactionCode();
        if (code == null) {
            return true;
        }
        int lastDash = code.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < code.length() - 1) {
            try {
                long createdMillis = Long.parseLong(code.substring(lastDash + 1));
                return System.currentTimeMillis() - createdMillis > Payment.PAYMENT_TIMEOUT_MINUTES * 60 * 1000L;
            } catch (NumberFormatException ignored) {}
        }
        return true;
    }

    @Override
    public Payment recordFinalPayment(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod) {
        return recordCounterPayment(bookingId, amount, paymentMethod, PaymentType.FINAL);
    }

    @Override
    public Payment recordRentalBalancePayment(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod) {
        return recordCounterPayment(bookingId, amount, paymentMethod, PaymentType.RENTAL_BALANCE);
    }

    private Payment recordCounterPayment(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod, PaymentType type) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentType(type);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionCode(type + "-" + bookingId + "-" + System.currentTimeMillis());
        payment.setPaidAt(OffsetDateTime.now());
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(PaymentReceivedEvent.of(booking, saved,
                PaymentType.RENTAL_BALANCE == type ? PaymentReceivedEvent.Kind.RENTAL_BALANCE : PaymentReceivedEvent.Kind.SETTLEMENT));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByPaidAtAsc(bookingId);
    }

    @Override
    @Transactional
    public Payment createPendingRefund(Long bookingId, BigDecimal amount, String bankCode, String accountNumber,
            String accountHolder) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));

        Payment refund = new Payment();
        refund.setBooking(booking);
        refund.setPaymentType(PaymentType.REFUND);
        refund.setAmount(amount != null ? amount
                : (booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO));
        refund.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        refund.setStatus(PaymentStatus.PENDING);

        String cleanBank = (bankCode != null && !bankCode.isBlank()) ? bankCode.trim().toUpperCase() : "BANK";
        String cleanAcc = (accountNumber != null && !accountNumber.isBlank()) ? accountNumber.trim() : "";
        String cleanHolder = (accountHolder != null && !accountHolder.isBlank()) ? accountHolder.trim().toUpperCase()
                : "";
        String refInfo = cleanBank + ": " + cleanAcc + " - " + cleanHolder;
        if (refInfo.length() > 100) {
            refInfo = refInfo.substring(0, 100);
        }
        refund.setTransactionCode(refInfo);

        return paymentRepository.save(refund);
    }

    @Override
    @Transactional
    public Payment confirmRefundCompleted(Long paymentId, Long employeeId, String transactionNo) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khoản thanh toán: " + paymentId));

        if (PaymentType.REFUND != payment.getPaymentType()) {
            throw new IllegalStateException("Giao dịch không phải là khoản hoàn tiền");
        }
        if (PaymentStatus.PENDING != payment.getStatus()) {
            return payment; // Idempotent
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionCode(transactionNo.trim());
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new RefundCompletedEvent(payment.getBooking().getId(), payment.getId()));
        return saved;
    }

    @Override
    @Transactional
    public Payment recordCounterRefund(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod) {
        if (paymentMethod != PaymentMethod.CASH && paymentMethod != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Hình thức hoàn tiền phải là tiền mặt hoặc chuyển khoản");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        Payment refund = new Payment();
        refund.setBooking(booking);
        refund.setPaymentType(PaymentType.REFUND);
        refund.setAmount(amount);
        refund.setPaymentMethod(paymentMethod);
        refund.setStatus(PaymentStatus.PAID);
        refund.setTransactionCode("REFUND-" + bookingId + "-" + System.currentTimeMillis());
        refund.setPaidAt(OffsetDateTime.now());
        Payment saved = paymentRepository.save(refund);
        eventPublisher.publishEvent(new RefundCompletedEvent(bookingId, saved.getId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPendingRefund(Long bookingId) {
        return paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(bookingId, PaymentType.REFUND, PaymentStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPendingRefunds() {
        return paymentRepository.findByPaymentTypeAndStatus(PaymentType.REFUND, PaymentStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingRefunds() {
        return paymentRepository.countPendingRefunds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPendingRefundBookingIds() {
        return paymentRepository.findBookingIdsWithPendingRefund();
    }
}
