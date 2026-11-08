package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Đổi xe before handover only ever gives another car of the booked type. When none is left the booking is cancelled
 * with a full deposit refund at the counter, so a late return can never be used to get a better car.
 */
class CarSwapTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private BookingServiceImpl service;

    private final VehicleType booked = type(1L, "100000");
    private final VehicleType pricier = type(2L, "350000");
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(
                bookingRepository, mock(CustomerRepository.class), mock(SlotRepository.class),
                mock(VehicleTypeRepository.class), vehicleRepository, mock(EmployeeRepository.class),
                mock(PromotionRepository.class), paymentService, mock(ContractService.class),
                mock(ContractTemplateService.class), mock(BookingFeeService.class), mock(PricingService.class));

        Slot slot = new Slot();
        slot.setVehicleType(booked);
        booking = new Booking();
        booking.setId(10L);
        booking.setSlot(slot);
        booking.setStatus("confirmed");
        booking.setPricePerHour(new BigDecimal("100000"));
        booking.setDepositAmount(new BigDecimal("500000"));
        // Pickup is 2 hours away, long past the 72-hour free-cancellation deadline.
        booking.setPickupTime(OffsetDateTime.now().plusHours(2));
        booking.setScheduledEndTime(OffsetDateTime.now().plusHours(6));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
    }

    @Test
    void anotherCarOfTheBookedTypeCanBeSwappedIn() {
        Vehicle sameType = car(11L, booked);
        when(vehicleRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(sameType));

        service.assignVehicle(10L, 11L);

        assertThat(booking.getVehicle()).isSameAs(sameType);
    }

    @Test
    void aCarOfAnotherTypeIsNeverSwappedInEvenAPricierOne() {
        when(vehicleRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(car(20L, pricier)));

        assertThatThrownBy(() -> service.assignVehicle(10L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cùng loại");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void withNoCarOfTheBookedTypeLeftTheWholeDepositIsHandedBackAtTheCounterEvenPastTheDeadline() {
        when(vehicleRepository.findByVehicleTypeIdWithDetails(1L)).thenReturn(List.of());

        service.cancelForUnavailableCar(10L, "CASH");

        assertThat(booking.getStatus()).isEqualTo("cancelled");
        verify(paymentService).recordCounterRefund(10L, new BigDecimal("500000"), "CASH");
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
    }

    @Test
    void theFullRefundIsRefusedWhileACarOfTheBookedTypeIsStillFree() {
        when(vehicleRepository.findByVehicleTypeIdWithDetails(1L)).thenReturn(List.of(car(11L, booked)));

        assertThatThrownBy(() -> service.cancelForUnavailableCar(10L, "CASH"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đổi xe");
        assertThat(booking.getStatus()).isEqualTo("confirmed");
        verify(paymentService, never()).recordCounterRefund(any(), any(), any());
    }

    @Test
    void aBookingAlreadyHandedOverCannotBeCancelledThisWay() {
        booking.setStatus("active");

        assertThatThrownBy(() -> service.cancelForUnavailableCar(10L, "CASH"))
                .isInstanceOf(IllegalStateException.class);
        verify(paymentService, never()).recordCounterRefund(any(), any(), any());
    }

    private static VehicleType type(Long id, String pricePerHour) {
        VehicleType type = new VehicleType();
        type.setId(id);
        type.setPricePerHour(new BigDecimal(pricePerHour));
        return type;
    }

    private static Vehicle car(Long id, VehicleType type) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(Vehicle.STATUS_AVAILABLE);
        vehicle.setVehicleType(type);
        return vehicle;
    }
}
