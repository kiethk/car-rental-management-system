package com.crms.mvc.service;

import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Returning the car: settle once, close the booking, and send the car to maintenance only if an issue says so. */
class BookingCheckOutTest {
    private static final BigDecimal LATE_FEE = new BigDecimal("75000");

    private BookingRepository bookingRepository;
    private VehicleRepository vehicleRepository;
    private PaymentService paymentService;
    private IssueService issueService;
    private BookingService service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        paymentService = mock(PaymentService.class);
        issueService = mock(IssueService.class);
        ContractService contractService = mock(ContractService.class);
        PricingService pricingService = mock(PricingService.class);
        service = new BookingServiceImpl(bookingRepository, mock(CustomerRepository.class), mock(SlotRepository.class),
                mock(VehicleTypeRepository.class), vehicleRepository, mock(EmployeeRepository.class),
                mock(PromotionRepository.class), paymentService, contractService, mock(ContractTemplateService.class),
                mock(BookingFeeService.class), pricingService, issueService);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        vehicle.setStatus(Vehicle.STATUS_RENTED);
        booking = new Booking();
        booking.setId(42L);
        booking.setStatus(Booking.STATUS_ACTIVE);
        booking.setVehicle(vehicle);
        booking.setScheduledEndTime(OffsetDateTime.now().minusHours(1));
        booking.setPricePerHour(new BigDecimal("150000"));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(new BigDecimal("2.00"));
        when(contractService.getTermsOfBooking(42L)).thenReturn(terms);
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(LATE_FEE);
    }

    @Test
    void theBookingIsSettledOnceWithItsLateFeeAndIsThenCompleted() {
        service.checkOut(42L, 2L, new CheckOutDTO());

        verify(paymentService).processSettlement(42L, LATE_FEE);
        assertEquals(Booking.STATUS_COMPLETED, booking.getStatus());
        assertNotNull(booking.getActualReturnTime());
        verify(bookingRepository).save(booking);
    }

    @Test
    void aCollisionOrDamageIssueSendsTheCarToMaintenance() {
        when(issueService.bookingHasVehicleDamage(42L)).thenReturn(true);

        service.checkOut(42L, 2L, new CheckOutDTO());

        assertEquals(Vehicle.STATUS_MAINTENANCE, booking.getVehicle().getStatus());
        verify(vehicleRepository).save(booking.getVehicle());
    }

    @Test
    void withoutADamagingIssueTheCarIsAvailableAgainWhateverTheNotesSay() {
        when(issueService.bookingHasVehicleDamage(42L)).thenReturn(false);
        CheckOutDTO form = new CheckOutDTO();
        form.setReturnNotes("small damage on the mirror, hỏng gương");

        service.checkOut(42L, 2L, form);

        assertEquals(Vehicle.STATUS_AVAILABLE, booking.getVehicle().getStatus());
    }

    @Test
    void onlyAnActiveBookingCanBeCheckedOut() {
        booking.setStatus(Booking.STATUS_COMPLETED);

        assertThrows(IllegalStateException.class, () -> service.checkOut(42L, 2L, new CheckOutDTO()));

        verify(paymentService, never()).processSettlement(eq(42L), any());
    }

    @Test
    void checkOutDoesNotReviveARetiredVehicle() {
        booking.getVehicle().setStatus(Vehicle.STATUS_RETIRED);
        when(issueService.bookingHasVehicleDamage(42L)).thenReturn(false);

        service.checkOut(42L, 2L, new CheckOutDTO());

        assertEquals(Vehicle.STATUS_RETIRED, booking.getVehicle().getStatus());
        verify(vehicleRepository, never()).save(booking.getVehicle());
    }
}
