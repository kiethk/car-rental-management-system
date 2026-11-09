package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.config.StationProperties;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.notification.CustomerMailer;
import com.crms.mvc.event.NoShowCancelledEvent;
import com.crms.mvc.notification.PickupNotifier;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.impl.NoShowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Khách không đến nhận xe: a paid booking waits its pickup wait, then is cancelled without refund. */
class NoShowTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 8, 12, 0, 0, 0, ZoneOffset.ofHours(7));

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final ContractService contracts = mock(ContractService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final NoShowServiceImpl service = new NoShowServiceImpl(bookings, contracts, events);

    private static ContractTemplateDTO terms() {
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setPickupWaitHours(2);
        return terms;
    }

    @Test
    void theWaitEndsTwoHoursAfterPickupOrAtTheReturnTimeWhicheverComesFirst() {
        OffsetDateTime pickup = NOW;
        assertThat(terms().pickupDeadline(pickup, pickup.plusHours(10))).isEqualTo(pickup.plusHours(2));
        assertThat(terms().pickupDeadline(pickup, pickup.plusHours(1))).isEqualTo(pickup.plusHours(1));
    }

    @Test
    void aBookingStillInsideItsWaitIsKept() {
        Booking booking = paidBooking(NOW.minusHours(1), NOW.plusHours(8));
        when(bookings.findConfirmedWithPickupBefore(NOW)).thenReturn(List.of(booking));

        assertThat(service.cancelNoShows(NOW)).isZero();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verifyNoInteractions(events);
    }

    @Test
    void aBookingPastItsWaitIsCancelledWithoutRefundAndTheCustomerIsTold() {
        Booking booking = paidBooking(NOW.minusHours(2), NOW.plusHours(6));
        when(bookings.findConfirmedWithPickupBefore(NOW)).thenReturn(List.of(booking));

        assertThat(service.cancelNoShows(NOW)).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW);
        verify(events).publishEvent(any(NoShowCancelledEvent.class));
    }

    @Test
    void aShortRentalIsCancelledAtItsReturnTimeEvenBeforeTheFullWait() {
        Booking booking = paidBooking(NOW.minusHours(1), NOW);
        when(bookings.findConfirmedWithPickupBefore(NOW)).thenReturn(List.of(booking));

        assertThat(service.cancelNoShows(NOW)).isEqualTo(1);
    }

    @Test
    void theCustomerIsToldTheRentalHasStartedAsSoonAsThePickupTimeComes() throws Exception {
        CustomerMailer mailer = mock(CustomerMailer.class);
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.url(anyString())).thenAnswer(inv -> "http://localhost:8081" + inv.getArgument(0));
        PickupNotifier notifier = new PickupNotifier(bookings, contracts, mailer, StationProperties.DEFAULT);
        Booking booking = paidBooking(NOW, NOW.plusHours(8));
        when(bookings.findConfirmedPickupsBetween(NOW.minusMinutes(1), NOW)).thenReturn(List.of(booking));

        assertThat(notifier.notifyRentalStartedBetween(NOW.minusMinutes(1), NOW)).isEqualTo(1);
        verify(mailer).send(eq("an@example.com"), contains("đã bắt đầu"), eq("mail/rental-started"),
                argThat((Map<String, Object> vars) -> Integer.valueOf(2).equals(vars.get("pickupWaitHours"))));
    }

    private Booking paidBooking(OffsetDateTime pickup, OffsetDateTime end) {
        VehicleType type = new VehicleType();
        type.setBrand("Toyota");
        type.setName("Vios");
        Slot slot = new Slot();
        slot.setVehicleType(type);
        Customer customer = new Customer();
        customer.setName("Nguyễn Văn An");
        customer.setEmail("an@example.com");
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSlot(slot);
        booking.setCustomer(customer);
        booking.setPickupTime(pickup);
        booking.setScheduledEndTime(end);
        booking.setDepositAmount(new BigDecimal("500000"));
        when(contracts.getTermsOfBooking(7L)).thenReturn(terms());
        return booking;
    }
}
