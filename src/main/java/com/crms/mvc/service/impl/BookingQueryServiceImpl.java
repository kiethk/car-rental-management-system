package com.crms.mvc.service.impl;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.BookingQueryService;
import com.crms.mvc.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingQueryServiceImpl implements BookingQueryService {
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final BookingDetailMapper mapper;

    public BookingQueryServiceImpl(BookingRepository bookingRepository, PaymentService paymentService, BookingDetailMapper mapper) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    @Override
    public List<BookingSummaryDTO> getMyBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(mapper::toSummary).toList();
    }

    @Override
    public BookingDetailDTO getBookingDetail(Long bookingId, Long customerId) {
        Booking b = find(bookingId);
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn đặt xe này");
        }
        return mapper.toDetail(b);
    }

    @Override
    public BookingDetailDTO getBookingDetailForAdmin(Long bookingId) {
        return mapper.toDetail(find(bookingId));
    }

    @Override
    public List<BookingSummaryDTO> getAllBookings(String status) {
        Set<Long> pendingRefunds = new HashSet<>(paymentService.getPendingRefundBookingIds());

        List<Booking> bookings;
        if ("refund".equalsIgnoreCase(status) || "refunds".equalsIgnoreCase(status) || "pending_refund".equalsIgnoreCase(status)) {
            bookings = bookingRepository.findBookingsWithPendingRefund();
        } else if (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim())) {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        } else {
            BookingStatus wanted = BookingStatus.from(status);
            bookings = wanted == null ? List.of() : bookingRepository.findByStatusOrderByCreatedAtDesc(wanted);
        }
        return bookings.stream().map(b -> mapper.toAdminSummary(b, pendingRefunds)).toList();
    }

    @Override
    public long countTotalBookings() {
        return bookingRepository.count();
    }

    @Override
    public long countOverdueRentals() {
        return bookingRepository.countOverdueRentals(OffsetDateTime.now());
    }

    private Booking find(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
    }
}
