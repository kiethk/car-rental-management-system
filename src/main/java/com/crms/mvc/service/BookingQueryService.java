package com.crms.mvc.service;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;

import java.util.List;

/** Read-only views of bookings for the customer's pages and the admin screens; nothing here changes a booking. */
public interface BookingQueryService {
    /** The customer's own bookings, newest first. */
    List<BookingSummaryDTO> getMyBookings(Long customerId);

    /** One booking for its owner; refused for anyone else (a null customer id skips the ownership check). */
    BookingDetailDTO getBookingDetail(Long bookingId, Long customerId);

    BookingDetailDTO getBookingDetailForAdmin(Long bookingId);

    /** All bookings, or those of one status ("refund" lists the ones waiting for a deposit refund). */
    List<BookingSummaryDTO> getAllBookings(String status);

    long countTotalBookings();

    /** Active rentals whose car is still out past its scheduled return time. */
    long countOverdueRentals();
}
