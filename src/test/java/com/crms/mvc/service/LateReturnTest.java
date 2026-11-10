package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.LateReturnDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.notification.CustomerMailer;
import com.crms.mvc.notification.LateReturnNotifier;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** An overdue rental shows its live late time and fee, and the customer is mailed once when charging starts. */
class LateReturnTest {

    private static final OffsetDateTime END = OffsetDateTime.of(2026, 11, 7, 14, 0, 0, 0, ZoneOffset.ofHours(7));
    private static final BigDecimal HOURLY = new BigDecimal("100000");
    private static final BigDecimal TWO_HUNDRED_PERCENT = new BigDecimal("2.00");
    private final PricingService pricing = new PricingServiceImpl();

    @Test
    void nothingIsLateBeforeTheScheduledReturn() {
        assertThat(pricing.describeLateReturn(END, END, HOURLY, TWO_HUNDRED_PERCENT)).isNull();
    }

    @Test
    void insideTheFirstHourTheRentalIsLateButNothingIsChargedYet() {
        LateReturnDTO late = pricing.describeLateReturn(END, END.plusMinutes(45), HOURLY, TWO_HUNDRED_PERCENT);

        assertThat(late.isInGrace()).isTrue();
        assertThat(late.currentFee()).isEqualByComparingTo("0");
        assertThat(late.nextChargeAt()).isEqualTo(END.plusHours(1));
        assertThat(late.getLateText()).isEqualTo("45 phút");
    }

    @Test
    void afterTwoFullLateHoursTwoHoursAreChargedAndTheThirdStartsNext() {
        LateReturnDTO late = pricing.describeLateReturn(END, END.plusMinutes(135), HOURLY, TWO_HUNDRED_PERCENT);

        assertThat(late.chargedHours()).isEqualTo(2);
        assertThat(late.currentFee()).isEqualByComparingTo("400000");
        assertThat(late.nextChargeAt()).isEqualTo(END.plusHours(3));
        assertThat(late.getLateText()).isEqualTo("2 giờ 15 phút");
    }

    @Test
    void theNextChargeTimeIsInVietnamTimeEvenWhenTheDatabaseReturnsUtc() {
        // 18:00 in Vietnam, stored and read back as 11:00 UTC.
        OffsetDateTime endUtc = END.withOffsetSameInstant(ZoneOffset.UTC);

        LateReturnDTO late = pricing.describeLateReturn(endUtc, endUtc.plusMinutes(4), HOURLY, TWO_HUNDRED_PERCENT);

        assertThat(late.nextChargeAt().getOffset()).isEqualTo(ZoneOffset.ofHours(7));
        assertThat(late.nextChargeAt().getHour()).isEqualTo(15);
    }

    @Test
    void theCustomerIsMailedOnceWhenTheFirstLateHourStartsToBeCharged() throws Exception {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerMailer mailer = configuredMailer();
        LateReturnNotifier notifier = notifier(bookings, mailer);

        // Run covering 14:59 → 15:00: charging of a rental due back at 14:00 starts at 15:00.
        OffsetDateTime from = END.plusMinutes(59);
        OffsetDateTime to = END.plusHours(1);
        when(bookings.findRentalsDueBetween(from.minusHours(1), to.minusHours(1))).thenReturn(List.of(overdueBooking()));

        assertThat(notifier.notifyFirstChargedLateHourBetween(from, to)).isEqualTo(1);
        verify(mailer).send(eq("an@example.com"), anyString(), eq("mail/late-return"), anyMap());
    }

    @Test
    void theCustomerIsRemindedOnceAsSoonAsTheReturnTimePasses() throws Exception {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerMailer mailer = configuredMailer();
        LateReturnNotifier notifier = notifier(bookings, mailer);

        // Run covering 13:59 → 14:00: the rental due back at 14:00 has just become overdue.
        OffsetDateTime from = END.minusMinutes(1);
        when(bookings.findRentalsDueBetween(from, END)).thenReturn(List.of(overdueBooking()));

        assertThat(notifier.notifyReturnTimePassedBetween(from, END)).isEqualTo(1);
        org.mockito.ArgumentCaptor<String> subject = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/late-return-reminder"), anyMap());
        assertThat(subject.getValue()).contains("#42").contains("phí trễ sẽ bắt đầu tính sau 1 giờ");
    }

    @Test
    void withoutMailConfiguredNothingIsSent() throws Exception {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerMailer mailer = mock(CustomerMailer.class);
        when(mailer.isConfigured()).thenReturn(false);
        when(bookings.findRentalsDueBetween(any(), any())).thenReturn(List.of(overdueBooking()));

        assertThat(notifier(bookings, mailer).notifyFirstChargedLateHourBetween(END, END.plusMinutes(1))).isZero();
        verify(mailer, never()).send(any(), any(), any(), anyMap());
    }

    @Test
    void aFailingSendDoesNotStopTheOthersFromBeingMailed() throws Exception {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerMailer mailer = configuredMailer();
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down")).doNothing()
                .when(mailer).send(any(), any(), any(), anyMap());
        when(bookings.findRentalsDueBetween(any(), any())).thenReturn(List.of(overdueBooking(), overdueBooking()));

        assertThat(notifier(bookings, mailer).notifyReturnTimePassedBetween(END.minusMinutes(1), END)).isEqualTo(1);
    }

    private static CustomerMailer configuredMailer() {
        CustomerMailer mailer = mock(CustomerMailer.class);
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.url(any())).thenAnswer(inv -> "http://localhost:8081" + inv.getArgument(0));
        return mailer;
    }

    private LateReturnNotifier notifier(BookingRepository bookings, CustomerMailer mailer) {
        ContractService contracts = mock(ContractService.class);
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(TWO_HUNDRED_PERCENT);
        when(contracts.getTermsOfBooking(any())).thenReturn(terms);
        return new LateReturnNotifier(bookings, contracts, pricing, mailer);
    }

    private static Booking overdueBooking() {
        VehicleType type = new VehicleType();
        type.setBrand("Toyota");
        type.setName("Vios");
        type.setPricePerHour(HOURLY);
        Slot slot = new Slot();
        slot.setVehicleType(type);
        Customer customer = new Customer();
        customer.setName("Nguyễn Văn An");
        customer.setEmail("an@example.com");
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setSlot(slot);
        booking.setCustomer(customer);
        booking.setPricePerHour(HOURLY);
        booking.setScheduledEndTime(END);
        return booking;
    }
}
