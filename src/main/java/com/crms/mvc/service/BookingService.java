package com.crms.mvc.service;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingService {
    /** clientIp is stored as clickwrap evidence on the contract. */
    Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp);

    /**
     * Checks a requested rental window: both times fall on a whole hour (minute :00), pickup is not in the past
     * and return is after pickup.
     * Throws IllegalArgumentException with a customer-facing message instead of silently adjusting the times.
     */
    void validateRentalSchedule(LocalDateTime start, LocalDateTime end);
    List<BookingSummaryDTO> getMyBookings(Long customerId);
    BookingDetailDTO getBookingDetail(Long bookingId, Long customerId);
    BookingDetailDTO getBookingDetailForAdmin(Long bookingId);
    List<BookingSummaryDTO> getAllBookings(String status);
    void cancelBooking(Long bookingId, Long customerId);
    boolean cancelBooking(Long bookingId, Long customerId, BookingCancelDTO dto);
    /**
     * Đổi xe before handover: only to another car of the booked type, so the contract is still fulfilled.
     * Never to another type, which would let a late return be used to get a better car at a cheaper price.
     */
    void assignVehicle(Long bookingId, Long vehicleId);

    /**
     * No car of the booked type is free for this booking's rental window: the rental company cannot fulfil the
     * contract, so cancel the confirmed booking and hand back 100% of the deposit at the counter (refundMethod CASH
     * or BANK_TRANSFER), regardless of the free-cancellation deadline, and issue the customer the single-use
     * voucher (CompensationVoucherService). Refused while a same-type car is free.
     */
    void cancelForUnavailableCar(Long bookingId, String refundMethod);

    long countTotalBookings();

    /** Active rentals whose car is still out past its scheduled return time. */
    long countOverdueRentals();
    Optional<Booking> findEntityById(Long id);

    /** Periodic task to cancel pending bookings exceeding 15-minute expiration */
    void cancelExpiredPendingBookings();

    // Merged from SlotService:
    List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Hour-by-hour schedule of one car for {@code days} days from {@code fromDate} (Vietnam time), using the same
     * rule as findAvailableVehiclesInTimeRange, with the 1-hour buffer around each booking marked separately.
     */
    VehicleScheduleDTO getVehicleSchedule(Long vehicleId, java.time.LocalDate fromDate, int days);
    List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category);
    Slot createSlotForBooking(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);
}