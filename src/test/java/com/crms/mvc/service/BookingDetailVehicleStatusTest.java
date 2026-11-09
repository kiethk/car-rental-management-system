package com.crms.mvc.service;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.crms.mvc.service.impl.BookingQueryServiceImpl;
import com.crms.mvc.service.impl.BookingDetailMapper;
import com.crms.mvc.config.StationProperties;

/** The admin booking page needs to know when the vehicle assigned to a booking has been retired. */
class BookingDetailVehicleStatusTest {
    private BookingRepository bookingRepository;
    private BookingQueryService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        service = new BookingQueryServiceImpl(bookingRepository, mock(PaymentService.class),
                new BookingDetailMapper(mock(PaymentService.class), mock(ContractService.class), mock(BookingFeeService.class),
                        mock(PricingService.class), StationProperties.DEFAULT));
    }

    @Test
    void detailFlagsABookingWhoseVehicleWasRetired() {
        givenConfirmedBookingWithVehicleStatus(Vehicle.STATUS_RETIRED);

        BookingDetailDTO detail = service.getBookingDetailForAdmin(42L);

        assertTrue(detail.isVehicleRetired());
    }

    @Test
    void detailDoesNotFlagABookingWhoseVehicleIsWorking() {
        givenConfirmedBookingWithVehicleStatus("available");

        BookingDetailDTO detail = service.getBookingDetailForAdmin(42L);

        assertFalse(detail.isVehicleRetired());
    }

    private void givenConfirmedBookingWithVehicleStatus(String vehicleStatus) {
        VehicleType type = new VehicleType();
        type.setId(1L);
        Slot slot = new Slot();
        slot.setId(1L);
        slot.setVehicleType(type);
        Customer customer = new Customer();
        customer.setId(5L);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        vehicle.setStatus(vehicleStatus);

        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        booking.setSlot(slot);
        booking.setVehicle(vehicle);
        booking.setStatus("confirmed");
        booking.setPickupTime(OffsetDateTime.now().plusDays(1));
        booking.setScheduledEndTime(OffsetDateTime.now().plusDays(2));
        booking.setPricePerHour(new BigDecimal("150000"));
        booking.setTotalPrice(new BigDecimal("1350000"));
        booking.setDepositAmount(new BigDecimal("675000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
    }
}
