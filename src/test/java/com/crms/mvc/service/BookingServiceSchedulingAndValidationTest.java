package com.crms.mvc.service;

import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.PasswordChangeDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Vehicle;
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

import java.time.OffsetDateTime;
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
    @DisplayName("B14: PasswordChangeDTO rejects new password shorter than 8 characters")
    void testPasswordChangeMinLength() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setCurrentPassword("oldPass123");
        dto.setNewPassword("123"); // < 8 chars

        Set<ConstraintViolation<PasswordChangeDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));

        dto.setNewPassword("newSecretPass");
        violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
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
                .hasMessageContaining("Only 'available' and 'maintenance' are allowed");

        assertThatThrownBy(() -> vehicleService.updateStatus(10L, "invalid_status"))
                .isInstanceOf(IllegalArgumentException.class);

        vehicleService.updateStatus(10L, "maintenance");
        assertThat(vehicle.getStatus()).isEqualTo("maintenance");
        verify(vehicleRepository, times(1)).save(vehicle);
    }
}
