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
    void assignVehicle(Long bookingId, Long vehicleId);

    long countTotalBookings();
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