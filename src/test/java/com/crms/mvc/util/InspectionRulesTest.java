package com.crms.mvc.util;

import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.enums.InspectionStatus;
import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.VehicleAvailabilityService;
import com.crms.mvc.service.VehicleInspectionService;
import com.crms.mvc.service.impl.VehicleAvailabilityServiceImpl;
import com.crms.mvc.service.impl.VehicleInspectionServiceImpl;
import com.crms.mvc.entity.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Inspection days block rentals, and an expiring registration needs an inspection scheduled before the rental. */
class InspectionRulesTest {
    private static final LocalDate EXPIRY = LocalDate.of(2026, 11, 10);

    private static LocalDate day(int d) {
        return LocalDate.of(2026, 11, d);
    }

    @Test
    void carWithValidRegistrationAndNoInspectionIsFree() {
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(), day(8), day(9))).isFalse();
        assertThat(InspectionRules.blocksRental(null, List.of(), day(20), day(21))).isFalse();
    }

    @Test
    void rentalEndingAfterExpiryIsBlockedWhenNoInspectionIsScheduled() {
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(), day(11), day(12))).isTrue();
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(), day(9), day(11))).isTrue();
    }

    @Test
    void rentalAfterAScheduledInspectionIsAllowedEvenPastTheOldExpiry() {
        // expiry 10, inspection on 9: customers can book 11 and 12
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(9)), day(11), day(12))).isFalse();
    }

    @Test
    void theWholeInspectionDayIsBlocked() {
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(9)), day(9), day(9))).isTrue();
        // a rental that starts the day before and returns on inspection day would delay it
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(9)), day(8), day(9))).isTrue();
        // returned the day before: fine
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(9)), day(7), day(8))).isFalse();
    }

    @Test
    void anInspectionAfterTheExpiryDoesNotOpenTheGapBeforeIt() {
        // expiry 10, inspection only on 13: a rental on 11-12 would be driven with an expired registration
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(13)), day(11), day(12))).isTrue();
        // but after the inspection the car is fine again
        assertThat(InspectionRules.blocksRental(EXPIRY, List.of(day(13)), day(14), day(15))).isFalse();
    }

    @Test
    void availabilityHidesACarWhoseRegistrationWouldExpireMidRental() {
        BookingRepository bookings = mock(BookingRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        VehicleAvailabilityService service = new VehicleAvailabilityServiceImpl(bookings, vehicles, null, null, null);
        LocalDate today = LocalDate.now();
        Vehicle expiring = car(1L, today.plusDays(1));
        Vehicle fine = car(2L, today.plusYears(1));
        when(vehicles.findByVehicleTypeIdWithDetails(5L)).thenReturn(List.of(expiring, fine));

        OffsetDateTime start = today.plusDays(3).atTime(9, 0).atOffset(ZoneOffset.ofHours(7));
        List<Vehicle> free = service.findAvailableVehiclesInTimeRange(5L, start, start.plusHours(5));

        assertThat(free).extracting(Vehicle::getId).containsExactly(2L);
    }

    @Test
    void aRentalEndingAtMidnightDoesNotDriveOnTheDayThatStartsThen() {
        BookingRepository bookings = mock(BookingRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        VehicleAvailabilityService service = new VehicleAvailabilityServiceImpl(bookings, vehicles, null, null, null);
        LocalDate today = LocalDate.now();
        // registration valid through day 3; the rental ends at 00:00 on day 4, so it is driven only on days 1-3
        Vehicle car = car(1L, today.plusDays(3));
        when(vehicles.findByVehicleTypeIdWithDetails(5L)).thenReturn(List.of(car));

        OffsetDateTime start = today.plusDays(1).atTime(12, 0).atOffset(ZoneOffset.ofHours(7));
        OffsetDateTime endsAtMidnight = today.plusDays(4).atStartOfDay().atOffset(ZoneOffset.ofHours(7));

        assertThat(service.findAvailableVehiclesInTimeRange(5L, start, endsAtMidnight)).extracting(Vehicle::getId).containsExactly(1L);
        assertThat(service.findAvailableVehiclesInTimeRange(5L, start, endsAtMidnight.plusHours(1))).isEmpty();
    }

    @Test
    void schedulingAnInspectionOnADayTheCarIsBookedIsRefused() {
        VehicleInspectionRepository inspections = mock(VehicleInspectionRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        VehicleInspectionService service = new VehicleInspectionServiceImpl(inspections, vehicles, bookings);
        Vehicle vehicle = car(7L, LocalDate.now().plusDays(30));
        when(vehicles.findByIdWithDetails(7L)).thenReturn(Optional.of(vehicle));
        when(bookings.findBookedVehicleIdsInTimeRange(any(), any(), any(), any())).thenReturn(List.of(7L));

        VehicleInspectionFormDTO form = new VehicleInspectionFormDTO();
        form.setVehicleId(7L);
        form.setScheduledDate(LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> service.schedule(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã có đơn thuê");
        verify(inspections, never()).save(any(VehicleInspection.class));
    }

    @Test
    void schedulingAnInspectionOnAFreeDaySucceeds() {
        VehicleInspectionRepository inspections = mock(VehicleInspectionRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        VehicleInspectionService service = new VehicleInspectionServiceImpl(inspections, vehicles, bookings);
        Vehicle vehicle = car(7L, LocalDate.now().plusDays(30));
        when(vehicles.findByIdWithDetails(7L)).thenReturn(Optional.of(vehicle));
        when(bookings.findBookedVehicleIdsInTimeRange(any(), any(), any(), any())).thenReturn(List.of(99L));
        when(inspections.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleInspectionFormDTO form = new VehicleInspectionFormDTO();
        form.setVehicleId(7L);
        form.setScheduledDate(LocalDate.now().plusDays(2));

        assertThat(service.schedule(form).getStatus()).isEqualTo(InspectionStatus.SCHEDULED);
    }

    private static Vehicle car(Long id, LocalDate registrationExpiry) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setRegistrationExpiry(registrationExpiry);
        vehicle.setVehicleType(new VehicleType());
        return vehicle;
    }
}
