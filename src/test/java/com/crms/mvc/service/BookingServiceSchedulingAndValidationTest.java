package com.crms.mvc.service;

import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.VehicleServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.dto.BookingCancelDTO;
import com.crms.mvc.dto.ContractTemplateDTO;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceSchedulingAndValidationTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ContractService contractService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("B9: Scheduled cancellation invokes repository with cutoff time around 15 minutes ago")
    void testCancelExpiredPendingBookingsTask() {
        when(bookingRepository.cancelExpiredPendingBookings(any(OffsetDateTime.now().getClass()))).thenReturn(2);

        bookingService.cancelExpiredPendingBookings();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingRepository, times(1)).cancelExpiredPendingBookings(captor.capture());

        OffsetDateTime cutoff = captor.getValue();
        OffsetDateTime now = OffsetDateTime.now();
        // Cutoff should be approximately 15 minutes before now
        assertThat(cutoff).isBefore(now.minusMinutes(14));
        assertThat(cutoff).isAfter(now.minusMinutes(16));
    }

    @Test
    @DisplayName("B14: CustomerRegisterDTO rejects password shorter than 8 characters")
    void testCustomerRegisterPasswordMinLength() {
        CustomerRegisterDTO dto = new CustomerRegisterDTO();
        dto.setName("John Doe");
        dto.setEmail("john@example.com");
        dto.setPhone("0912345678");
        dto.setPassword("short"); // < 8 chars

        Set<ConstraintViolation<CustomerRegisterDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        dto.setPassword("validPass123");
        violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("B14: AccountProfileDTO rejects new password shorter than 8 characters")
    void testPasswordChangeMinLength() {
        AccountProfileDTO dto = new AccountProfileDTO();
        dto.setCurrentPassword("oldPass123");
        dto.setNewPassword("123"); // < 8 chars
        dto.setConfirmPassword("123");

        Set<ConstraintViolation<AccountProfileDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPasswordLongEnough"));

        dto.setNewPassword("newSecretPass");
        dto.setConfirmPassword("newSecretPass");
        violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("newPasswordLongEnough"));
    }

    @Test
    @DisplayName("B14: VehicleFormDTO rejects status other than available and maintenance")
    void testVehicleFormDTOStatusRestriction() {
        VehicleFormDTO dto = new VehicleFormDTO();
        dto.setPlate("51H-12345");
        dto.setColor("Black");
        dto.setVehicleTypeId(1L);
        dto.setStatus("rented"); // Form cannot set status directly to rented

        Set<ConstraintViolation<VehicleFormDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));

        dto.setStatus("available");
        violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("status"));

        dto.setStatus("maintenance");
        violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

    @Test
    @DisplayName("B14: VehicleServiceImpl.updateStatus rejects manual rented or invalid status")
    void testVehicleServiceUpdateStatusValidation() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setStatus("available");

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateStatus(10L, "rented"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only 'available', 'maintenance' and 'retired' are allowed");

        assertThatThrownBy(() -> vehicleService.updateStatus(10L, "invalid_status"))
                .isInstanceOf(IllegalArgumentException.class);

        vehicleService.updateStatus(10L, "maintenance");
        assertThat(vehicle.getStatus()).isEqualTo("maintenance");
        verify(vehicleRepository, times(1)).save(vehicle);
    }

    @Test
    @DisplayName("Cancel confirmed booking > 72h before pickup creates pending refund")
    void testCancelConfirmedBookingEligibleForRefund() {
        Long bookingId = 100L;
        Long customerId = 1L;

        Customer customer = new Customer();
        customer.setId(customerId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomer(customer);
        booking.setStatus("confirmed");
        booking.setDepositAmount(new BigDecimal("500000"));
        booking.setPickupTime(OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(5));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setFreeCancellationHours(72);
        when(contractService.getTermsOfBooking(bookingId)).thenReturn(terms);

        BookingCancelDTO dto = new BookingCancelDTO();
        dto.setBankCode("VCB");
        dto.setAccountNumber("0123456789");
        dto.setAccountHolder("NGUYEN VAN A");

        boolean refundCreated = bookingService.cancelBooking(bookingId, customerId, dto);

        assertThat(refundCreated).isTrue();
        assertThat(booking.getStatus()).isEqualTo("cancelled");
        verify(paymentService, times(1)).createPendingRefund(bookingId, new BigDecimal("500000"), "VCB", "0123456789", "NGUYEN VAN A");
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("Cancel confirmed booking < 72h before pickup does not create refund")
    void testCancelConfirmedBookingIneligibleForRefund() {
        Long bookingId = 101L;
        Long customerId = 1L;

        Customer customer = new Customer();
        customer.setId(customerId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomer(customer);
        booking.setStatus("confirmed");
        booking.setDepositAmount(new BigDecimal("500000"));
        booking.setPickupTime(OffsetDateTime.now(ZoneOffset.ofHours(7)).plusHours(10)); // < 72h

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setFreeCancellationHours(72);
        when(contractService.getTermsOfBooking(bookingId)).thenReturn(terms);

        BookingCancelDTO dto = new BookingCancelDTO();
        dto.setBankCode("VCB");
        dto.setAccountNumber("0123456789");
        dto.setAccountHolder("NGUYEN VAN A");

        boolean refundCreated = bookingService.cancelBooking(bookingId, customerId, dto);

        assertThat(refundCreated).isFalse();
        assertThat(booking.getStatus()).isEqualTo("cancelled");
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("G7: 0 free-cancellation hours refunds right up to pickup instead of falling back to 72h")
    void testZeroFreeCancellationHoursRefundsUntilPickup() {
        Customer customer = new Customer();
        customer.setId(1L);
        Booking booking = new Booking();
        booking.setId(103L);
        booking.setCustomer(customer);
        booking.setStatus("confirmed");
        booking.setDepositAmount(new BigDecimal("500000"));
        booking.setPickupTime(OffsetDateTime.now().plusHours(2));
        when(bookingRepository.findById(103L)).thenReturn(Optional.of(booking));

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setFreeCancellationHours(0);
        when(contractService.getTermsOfBooking(103L)).thenReturn(terms);

        assertThat(bookingService.cancelBooking(103L, 1L, null)).isTrue();
        verify(paymentService).createPendingRefund(eq(103L), eq(new BigDecimal("500000")), any(), any(), any());
    }

    @Test
    @DisplayName("G7: missing accepted terms fails the cancellation instead of guessing 72h")
    void testMissingAcceptedTermsFailsLoudly() {
        Customer customer = new Customer();
        customer.setId(1L);
        Booking booking = new Booking();
        booking.setId(104L);
        booking.setCustomer(customer);
        booking.setStatus("confirmed");
        booking.setPickupTime(OffsetDateTime.now().plusDays(5));
        when(bookingRepository.findById(104L)).thenReturn(Optional.of(booking));
        when(contractService.getTermsOfBooking(104L))
                .thenThrow(new java.util.NoSuchElementException("Contract not found for booking: 104"));

        assertThatThrownBy(() -> bookingService.cancelBooking(104L, 1L, null))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(booking.getStatus()).isEqualTo("confirmed");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel pending_payment booking cancels without refund")
    void testCancelPendingPaymentBooking() {
        Long bookingId = 102L;
        Long customerId = 1L;

        Customer customer = new Customer();
        customer.setId(customerId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomer(customer);
        booking.setStatus("pending_payment");
        booking.setDepositAmount(new BigDecimal("500000"));
        booking.setPickupTime(OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(5));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        boolean refundCreated = bookingService.cancelBooking(bookingId, customerId, null);

        assertThat(refundCreated).isFalse();
        assertThat(booking.getStatus()).isEqualTo("cancelled");
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("A retired vehicle cannot be assigned to a booking")
    void testAssignRetiredVehicleIsRejected() {
        VehicleType type = new VehicleType();
        type.setId(3L);
        Slot slot = new Slot();
        slot.setVehicleType(type);
        Booking booking = new Booking();
        booking.setId(200L);
        booking.setSlot(slot);
        booking.setStatus("confirmed");
        Vehicle retired = new Vehicle();
        retired.setId(9L);
        retired.setVehicleType(type);
        retired.setStatus(Vehicle.STATUS_RETIRED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(booking));
        when(vehicleRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> bookingService.assignVehicle(200L, 9L))
                .isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("A car cannot be swapped once it has been handed over")
    void testSwapAfterHandoverIsRejected() {
        Booking booking = new Booking();
        booking.setId(201L);
        booking.setStatus("active");
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.assignVehicle(201L, 9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trước khi bàn giao");

        verify(vehicleRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vehicles offered for a time range exclude retired and maintenance ones")
    void testAvailableVehiclesExcludeRetiredAndMaintenance() {
        Vehicle ready = vehicleWithStatus(1L, "available");
        Vehicle repairing = vehicleWithStatus(2L, "maintenance");
        Vehicle retired = vehicleWithStatus(3L, Vehicle.STATUS_RETIRED);
        when(vehicleRepository.findByVehicleTypeIdWithDetails(3L)).thenReturn(java.util.List.of(ready, repairing, retired));

        var offered = bookingService.findAvailableVehiclesInTimeRange(3L, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2));

        assertThat(offered).containsExactly(ready);
    }

    private static Vehicle vehicleWithStatus(Long id, String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(status);
        return vehicle;
    }

    private static java.time.LocalDateTime wholeHourTomorrow(int hour) {
        return java.time.LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Rental schedule: whole-hour pickup and return in the future is accepted")
    void testRentalScheduleAcceptsWholeHours() {
        java.time.LocalDateTime start = wholeHourTomorrow(9);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> bookingService.validateRentalSchedule(start, start.plusHours(9)));
    }

    @Test
    @DisplayName("Rental schedule: pickup that is not on the hour is rejected")
    void testRentalScheduleRejectsPickupNotOnTheHour() {
        java.time.LocalDateTime start = wholeHourTomorrow(9).withMinute(30);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(start, wholeHourTomorrow(18)));
    }

    @Test
    @DisplayName("Rental schedule: return that is not on the hour is rejected")
    void testRentalScheduleRejectsReturnNotOnTheHour() {
        java.time.LocalDateTime end = wholeHourTomorrow(18).withMinute(15);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(wholeHourTomorrow(9), end));
    }

    @Test
    @DisplayName("Rental schedule: seconds or nanoseconds past the hour are rejected")
    void testRentalScheduleRejectsSecondsPastTheHour() {
        java.time.LocalDateTime withSeconds = wholeHourTomorrow(9).withSecond(30);
        java.time.LocalDateTime withNanos = wholeHourTomorrow(9).withNano(1);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(withSeconds, wholeHourTomorrow(18)));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(withNanos, wholeHourTomorrow(18)));
    }

    @Test
    @DisplayName("Rental schedule: pickup in the past is rejected")
    void testRentalScheduleRejectsPickupInThePast() {
        java.time.LocalDateTime start = wholeHourTomorrow(9).minusDays(2);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(start, start.plusHours(9)));
    }

    @Test
    @DisplayName("Rental schedule: return at or before pickup is rejected")
    void testRentalScheduleRejectsReturnNotAfterPickup() {
        java.time.LocalDateTime start = wholeHourTomorrow(12);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(start, start));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> bookingService.validateRentalSchedule(start, start.minusHours(3)));
    }
}
