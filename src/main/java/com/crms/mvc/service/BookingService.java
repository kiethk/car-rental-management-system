package com.crms.mvc.service;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingService {
    /** clientIp is stored as clickwrap evidence on the contract. */
    Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp);
    List<BookingSummaryDTO> getMyBookings(Long customerId);
    BookingDetailDTO getBookingDetail(Long bookingId, Long customerId);
    BookingDetailDTO getBookingDetailForAdmin(Long bookingId);
    List<BookingSummaryDTO> getAllBookings(String status);
    void cancelBooking(Long bookingId, Long customerId);
    boolean cancelBooking(Long bookingId, Long customerId, BookingCancelDTO dto);
    void assignVehicle(Long bookingId, Long vehicleId);
    void checkIn(Long bookingId, Long employeeId);
    void checkOut(Long bookingId, Long employeeId, CheckOutDTO dto);

    long countTotalBookings();
    Optional<Booking> findEntityById(Long id);

    /** Periodic task to cancel pending bookings exceeding 15-minute expiration */
    void cancelExpiredPendingBookings();

    // Merged from SlotService:
    List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);
    List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category);
    Slot createSlotForBooking(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);
}
