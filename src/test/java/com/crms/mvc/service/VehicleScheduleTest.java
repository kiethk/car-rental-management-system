package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleScheduleDTO;
import com.crms.mvc.dto.VehicleScheduleDTO.HourState;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
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
    private BookingServiceImpl service;
    private final LocalDate day = LocalDate.now(VN).plusDays(2);

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(
                bookingRepository, mock(CustomerRepository.class), mock(SlotRepository.class),
                mock(VehicleTypeRepository.class), vehicleRepository, mock(EmployeeRepository.class),
                mock(PromotionRepository.class), mock(PaymentService.class), mock(ContractService.class),
                mock(ContractTemplateService.class), mock(BookingFeeService.class), mock(PricingService.class), mock(CompensationVoucherService.class));

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
