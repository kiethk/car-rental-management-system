package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.VehicleAvailabilityServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Huỷ miễn phí: before the deadline ahead of pickup, or within the Thời gian huỷ sau cọc, never past pickup. */
class FreeCancellationTest {

    private static final OffsetDateTime PICKUP = OffsetDateTime.of(2026, 11, 10, 10, 0, 0, 0, ZoneOffset.ofHours(7));

    private static ContractTemplateDTO terms() {
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setFreeCancellationHours(72);
        terms.setFreeCancelAfterDepositHours(1);
        return terms;
    }

    @Test
    void withoutADepositOnlyTheDeadlineBeforePickupCounts() {
        assertThat(terms().freeCancellationUntil(PICKUP, null)).isEqualTo(PICKUP.minusHours(72));
    }

    @Test
    void aBookingMadeInsideTheNoRefundPeriodCanStillBeCancelledFreelyForOneHourAfterTheDeposit() {
        OffsetDateTime paid = PICKUP.minusHours(24);

        assertThat(terms().freeCancellationUntil(PICKUP, paid)).isEqualTo(paid.plusHours(1));
    }

    @Test
    void theWindowAfterTheDepositNeverRunsPastPickup() {
        OffsetDateTime paid = PICKUP.minusMinutes(30);

        assertThat(terms().freeCancellationUntil(PICKUP, paid)).isEqualTo(PICKUP);
    }

    @Test
    void anEarlyDepositKeepsTheLaterDeadlineBeforePickup() {
        OffsetDateTime paid = PICKUP.minusDays(10);

        assertThat(terms().freeCancellationUntil(PICKUP, paid)).isEqualTo(PICKUP.minusHours(72));
    }

    @Test
    void cancellingTenMinutesAfterTheDepositRefundsEvenOneDayBeforePickup() {
        BookingRepository bookings = mock(BookingRepository.class);
        PaymentService payments = mock(PaymentService.class);
        ContractService contracts = mock(ContractService.class);
        BookingServiceImpl service = new BookingServiceImpl(bookings, mock(CustomerRepository.class), mock(SlotRepository.class), mock(VehicleTypeRepository.class), mock(VehicleRepository.class), mock(EmployeeRepository.class), mock(PromotionRepository.class), payments, contracts, mock(ContractTemplateService.class), mock(PricingService.class), new VehicleAvailabilityServiceImpl(bookings, mock(VehicleRepository.class), mock(VehicleTypeRepository.class), mock(ContractTemplateService.class), mock(PricingService.class)));

        Customer customer = new Customer();
        customer.setId(1L);
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDepositAmount(new BigDecimal("500000"));
        booking.setPickupTime(OffsetDateTime.now().plusHours(24));
        when(bookings.findById(5L)).thenReturn(Optional.of(booking));
        when(contracts.getTermsOfBooking(5L)).thenReturn(terms());
        Payment deposit = new Payment();
        deposit.setPaymentType(PaymentType.DEPOSIT);
        deposit.setStatus(PaymentStatus.PAID);
        deposit.setPaidAt(OffsetDateTime.now().minusMinutes(10));
        when(payments.getPaymentsByBooking(5L)).thenReturn(List.of(deposit));

        assertThat(service.cancelBooking(5L, 1L, null)).isTrue();
        verify(payments).createPendingRefund(eq5(), any(), any(), any(), any());
    }

    private static Long eq5() {
        return org.mockito.ArgumentMatchers.eq(5L);
    }
}
