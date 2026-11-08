package com.crms.mvc.service;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.LateReturnDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.event.LateReturnNotifier;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.impl.PricingServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void theCustomerIsMailedOnceWhenTheFirstLateHourStartsToBeCharged() {
        BookingRepository bookings = mock(BookingRepository.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        LateReturnNotifier notifier = notifier(bookings, sender, "shop@example.com");

        // Run covering 14:59 → 15:00: charging of a rental due back at 14:00 starts at 15:00.
        OffsetDateTime from = END.plusMinutes(59);
        OffsetDateTime to = END.plusHours(1);
        when(bookings.findRentalsDueBetween(from.minusHours(1), to.minusHours(1))).thenReturn(List.of(overdueBooking()));

        assertThat(notifier.notifyFirstChargedLateHourBetween(from, to)).isEqualTo(1);
        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void theCustomerIsRemindedOnceAsSoonAsTheReturnTimePasses() throws Exception {
        BookingRepository bookings = mock(BookingRepository.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        LateReturnNotifier notifier = notifier(bookings, sender, "shop@example.com");

        // Run covering 13:59 → 14:00: the rental due back at 14:00 has just become overdue.
        OffsetDateTime from = END.minusMinutes(1);
        when(bookings.findRentalsDueBetween(from, END)).thenReturn(List.of(overdueBooking()));

        assertThat(notifier.notifyReturnTimePassedBetween(from, END)).isEqualTo(1);
        org.mockito.ArgumentCaptor<MimeMessage> mail = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(mail.capture());
        assertThat(mail.getValue().getSubject()).contains("#42").contains("phí trễ sẽ bắt đầu tính sau 1 giờ");
    }

    @Test
    void withoutMailConfiguredNothingIsSent() {
        BookingRepository bookings = mock(BookingRepository.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(bookings.findRentalsDueBetween(any(), any())).thenReturn(List.of(overdueBooking()));

        assertThat(notifier(bookings, sender, "").notifyFirstChargedLateHourBetween(END, END.plusMinutes(1))).isZero();
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @SuppressWarnings("unchecked")
    private LateReturnNotifier notifier(BookingRepository bookings, JavaMailSender sender, String username) {
        ContractService contracts = mock(ContractService.class);
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(TWO_HUNDRED_PERCENT);
        when(contracts.getTermsOfBooking(any())).thenReturn(terms);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        ITemplateEngine engine = mock(ITemplateEngine.class);
        when(engine.process(anyString(), any(org.thymeleaf.context.IContext.class))).thenReturn("<p>body</p>");
        return new LateReturnNotifier(bookings, contracts, pricing, provider, engine,
                username, "secret", "DriveNow <shop@example.com>", "http://localhost:8081");
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
        booking.setStatus("active");
        booking.setSlot(slot);
        booking.setCustomer(customer);
        booking.setPricePerHour(HOURLY);
        booking.setScheduledEndTime(END);
        return booking;
    }
}
