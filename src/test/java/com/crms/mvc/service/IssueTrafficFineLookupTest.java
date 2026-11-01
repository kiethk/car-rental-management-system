package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueEvidenceRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.service.impl.IssueServiceImpl;
import com.crms.mvc.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A traffic fine names a plate and a moment, not a booking. The plate may have moved to another car since,
 * so the car is the one that carried the plate at that moment, and the customer is whoever rented it then.
 */
class IssueTrafficFineLookupTest {
    private static final LocalDateTime VIOLATION = LocalDateTime.of(2026, 10, 20, 14, 30);

    private BookingRepository bookingRepository;
    private LicensePlateRepository plateRepository;
    private IssueService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        plateRepository = mock(LicensePlateRepository.class);
        service = new IssueServiceImpl(mock(IssueRepository.class), bookingRepository, mock(EmployeeRepository.class),
                mock(BookingFeeRepository.class), mock(IssueEvidenceRepository.class), mock(FileUploadService.class),
                plateRepository, mock(ApplicationEventPublisher.class));
    }

    @Test
    void theBookingIsTheRentalOfTheCarThatCarriedThePlateAtThatMoment() {
        Vehicle vehicle = vehicle(9L);
        plate("51A-123.45", vehicle, "2026-01-01T00:00+07:00", null);
        Booking rental = new Booking();
        rental.setId(42L);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of(rental));

        Booking found = service.findBookingForViolation("51A-123.45", VIOLATION);

        assertSame(rental, found);
        OffsetDateTime expectedInstant = VIOLATION.atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
        verify(bookingRepository).findRentalsCovering(9L, expectedInstant);
    }

    @Test
    void thePlateIsMatchedWithoutCaseOrSurroundingSpaces() {
        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", null);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of(new Booking()));

        service.findBookingForViolation("  51a-123.45 ", VIOLATION);

        verify(plateRepository).findByPlateIgnoreCase("51A-123.45".toLowerCase());
    }

    @Test
    void anUnknownPlateHasNoBooking() {
        when(plateRepository.findByPlateIgnoreCase("99Z-000.00")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("99Z-000.00", VIOLATION));

        verify(bookingRepository, never()).findRentalsCovering(any(), any());
    }

    @Test
    void aPlateThatWasNotOnTheCarYetOrWasAlreadyRetiredHasNoBooking() {
        plate("51A-123.45", vehicle(9L), "2026-11-01T00:00+07:00", null);
        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", "2026-10-01T00:00+07:00");
        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        verify(bookingRepository, never()).findRentalsCovering(any(), any());
    }

    @Test
    void aCarThatWasNotRentedAtThatMomentHasNoBookingAndTheCompanyBearsTheFine() {
        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", null);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of());

        NoSuchElementException error = assertThrows(NoSuchElementException.class,
                () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        assertTrue(error.getMessage().contains("Không có đơn thuê"));
    }

    private Vehicle vehicle(Long id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        return vehicle;
    }

    private void plate(String plate, Vehicle vehicle, String validFrom, String validTo) {
        LicensePlate row = new LicensePlate();
        row.setPlate(plate);
        row.setVehicle(vehicle);
        row.setValidFrom(OffsetDateTime.parse(validFrom));
        row.setValidTo(validTo == null ? null : OffsetDateTime.parse(validTo));
        when(plateRepository.findByPlateIgnoreCase(any())).thenReturn(Optional.of(row));
    }
}
