package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleScheduleDTO;
import com.crms.mvc.dto.VehicleScheduleDTO.HourState;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.VehicleAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VehicleScheduleTest {

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private VehicleAvailabilityServiceImpl service;
    private final LocalDate day = LocalDate.now(VN).plusDays(2);

    @BeforeEach
    void setUp() {
        service = new VehicleAvailabilityServiceImpl(bookingRepository, vehicleRepository, mock(VehicleTypeRepository.class),
                mock(ContractTemplateService.class), mock(PricingService.class));

        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setBrand("Toyota");
        type.setName("Vios");
        Vehicle car = new Vehicle();
        car.setId(5L);
        car.setStatus(Vehicle.STATUS_AVAILABLE);
        car.setVehicleType(type);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(car));
    }

    @Test
    void aBookingIsShownWithOneBufferHourOnEachSide() {
        Booking booking = new Booking();
        booking.setStatus("confirmed");
        booking.setPickupTime(day.atTime(10, 0).atOffset(VN));
        booking.setScheduledEndTime(day.atTime(14, 0).atOffset(VN));
        when(bookingRepository.findBookingsHoldingVehicle(eq(5L), any(), any(), any())).thenReturn(List.of(booking));

        VehicleScheduleDTO schedule = service.getVehicleSchedule(5L, day, 1);

        List<HourState> hours = schedule.getDays().get(0).hours();
        assertThat(hours).hasSize(24);
        assertThat(hours.get(8)).isEqualTo(HourState.FREE);
        assertThat(hours.get(9)).isEqualTo(HourState.BUFFER);
        assertThat(hours.subList(10, 14)).containsOnly(HourState.BOOKED);
        assertThat(hours.get(14)).isEqualTo(HourState.BUFFER);
        assertThat(hours.get(15)).isEqualTo(HourState.FREE);
    }

    @Test
    void daysAfterTheRegistrationExpiredAreMarkedWhenNoInspectionIsScheduled() {
        Vehicle car = vehicleRepository.findById(5L).get();
        car.setRegistrationExpiry(day.plusDays(1));

        VehicleScheduleDTO schedule = service.getVehicleSchedule(5L, day, 4);

        // expiry day is still valid; from the day after it, nothing can be booked
        assertThat(schedule.getDays().get(0).hours()).containsOnly(HourState.FREE);
        assertThat(schedule.getDays().get(1).hours()).containsOnly(HourState.FREE);
        assertThat(schedule.getDays().get(2).hours()).containsOnly(HourState.EXPIRED);
        assertThat(schedule.getDays().get(3).hours()).containsOnly(HourState.EXPIRED);
    }

    @Test
    void aScheduledInspectionClosesItsDayAndOpensTheDaysAfterIt() {
        Vehicle car = vehicleRepository.findById(5L).get();
        car.setRegistrationExpiry(day.plusDays(1));
        com.crms.mvc.entity.VehicleInspection inspection = new com.crms.mvc.entity.VehicleInspection();
        inspection.setVehicle(car);
        inspection.setScheduledDate(day.plusDays(1));
        inspection.setStatus("scheduled");
        when(vehicleRepository.findScheduledInspectionsByVehicleId(5L)).thenReturn(List.of(inspection));

        VehicleScheduleDTO schedule = service.getVehicleSchedule(5L, day, 3);

        assertThat(schedule.getDays().get(0).hours()).containsOnly(HourState.FREE);
        assertThat(schedule.getDays().get(1).hours()).containsOnly(HourState.INSPECTION);
        // inspected the day before, so the car is bookable again even though the old expiry has passed
        assertThat(schedule.getDays().get(2).hours()).containsOnly(HourState.FREE);
    }

    @Test
    void aBookedHourStaysBookedEvenOnAnInspectionDay() {
        Vehicle car = vehicleRepository.findById(5L).get();
        com.crms.mvc.entity.VehicleInspection inspection = new com.crms.mvc.entity.VehicleInspection();
        inspection.setVehicle(car);
        inspection.setScheduledDate(day);
        inspection.setStatus("scheduled");
        when(vehicleRepository.findScheduledInspectionsByVehicleId(5L)).thenReturn(List.of(inspection));
        Booking booking = new Booking();
        booking.setStatus("confirmed");
        booking.setPickupTime(day.atTime(10, 0).atOffset(VN));
        booking.setScheduledEndTime(day.atTime(14, 0).atOffset(VN));
        when(bookingRepository.findBookingsHoldingVehicle(eq(5L), any(), any(), any())).thenReturn(List.of(booking));

        List<HourState> hours = service.getVehicleSchedule(5L, day, 1).getDays().get(0).hours();

        assertThat(hours.subList(10, 14)).containsOnly(HourState.BOOKED);
        assertThat(hours.get(8)).isEqualTo(HourState.INSPECTION);
    }

    @Test
    void theHourWeAreInCannotStartARentalButTheNextOneCan() {
        OffsetDateTime now = OffsetDateTime.now(VN);
        int currentHour = now.getHour();

        VehicleScheduleDTO schedule = service.getVehicleSchedule(5L, now.toLocalDate(), 2);

        List<HourState> today = schedule.getDays().get(0).hours();
        assertThat(today.get(currentHour)).isEqualTo(HourState.PAST);
        if (currentHour < 23) {
            assertThat(today.get(currentHour + 1)).isEqualTo(HourState.FREE);
        } else {
            assertThat(schedule.getDays().get(1).hours().get(0)).isEqualTo(HourState.FREE);
        }
    }

    @Test
    void aCarNotBackFromAnOverdueRentalStaysBookedUntilNow() {
        Booking overdue = new Booking();
        overdue.setStatus("active");
        overdue.setPickupTime(OffsetDateTime.now().minusHours(6));
        overdue.setScheduledEndTime(OffsetDateTime.now().minusHours(2));
        when(bookingRepository.findBookingsHoldingVehicle(eq(5L), any(), any(), any())).thenReturn(List.of(overdue));

        VehicleScheduleDTO schedule = service.getVehicleSchedule(5L, LocalDate.now(VN), 1);

        int currentHour = OffsetDateTime.now().atZoneSameInstant(VN).getHour();
        assertThat(schedule.getDays().get(0).hours().get(currentHour)).isEqualTo(HourState.BOOKED);
    }
}
