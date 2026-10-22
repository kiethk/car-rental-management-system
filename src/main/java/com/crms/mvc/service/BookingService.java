package com.crms.mvc.service;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.Booking;
import java.util.List;

public interface BookingService {
    Booking createBooking(Long customerId, BookingCreateDTO dto);
    List<BookingSummaryDTO> getMyBookings(Long customerId);
    BookingDetailDTO getBookingDetail(Long bookingId, Long customerId);
    BookingDetailDTO getBookingDetailForAdmin(Long bookingId);
    List<BookingSummaryDTO> getAllBookings(String status);
    void cancelBooking(Long bookingId, Long customerId);
    void assignVehicle(Long bookingId, Long vehicleId);
    void checkIn(Long bookingId, Long employeeId);
    void checkOut(Long bookingId, Long employeeId, CheckOutDTO dto);
}

