package com.crms.mvc.service;

import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.*;
import com.crms.mvc.event.CompensationVoucherIssuedEvent;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.CompensationVoucherServiceImpl;
import com.crms.mvc.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Voucher bù: issued once per booking cancelled for lack of a car, 10% off, single use, valid 90 days. */
class CompensationVoucherTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final PromotionRepository promotions = mock(PromotionRepository.class);

    @Test
    void aVoucherIsTenPercentSingleUseForNinetyDaysAndIsEmailed() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        when(promotions.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));
        Customer customer = new Customer();
        customer.setName("Nguyễn Văn An");
        customer.setEmail("an@example.com");
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);

        // The voucher follows the contract the customer accepted: 10% for 90 days.
        ContractService contracts = mock(ContractService.class);
        ContractTemplateDTO accepted = new ContractTemplateDTO();
        accepted.setCompensationVoucherPercent(10);
        accepted.setCompensationVoucherDays(90);
        when(contracts.getTermsOfBooking(42L)).thenReturn(accepted);

        Promotion voucher = new CompensationVoucherServiceImpl(promotions, contracts, events).issueFor(booking);

        assertThat(voucher.getCode()).matches("BU42-[A-HJ-NP-Z2-9]{6}");
        assertThat(voucher.getDiscountType()).isEqualTo("percent");
        assertThat(voucher.getDiscountValue()).isEqualByComparingTo("10");
        assertThat(voucher.getQuantity()).isEqualTo(1);
        assertThat(voucher.getStartDate()).isEqualTo(TODAY);
        assertThat(voucher.getEndDate()).isEqualTo(TODAY.plusDays(89));
        ArgumentCaptor<CompensationVoucherIssuedEvent> sent = ArgumentCaptor.forClass(CompensationVoucherIssuedEvent.class);
        verify(events).publishEvent(sent.capture());
        assertThat(sent.getValue().code()).isEqualTo(voucher.getCode());
        assertThat(sent.getValue().customerEmail()).isEqualTo("an@example.com");
    }

    // ---- using a code at checkout

    private BookingServiceImpl service;
    private BookingRepository bookings;

    @BeforeEach
    void setUpCheckout() {
        bookings = mock(BookingRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        VehicleTypeRepository types = mock(VehicleTypeRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        SlotRepository slots = mock(SlotRepository.class);
        ContractTemplateService templates = mock(ContractTemplateService.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        service = new BookingServiceImpl(bookings, customers, slots, types, vehicles, employees, promotions,
                mock(PaymentService.class), mock(ContractService.class), templates, mock(BookingFeeService.class),
                new PricingServiceImpl(), mock(CompensationVoucherService.class));

        when(customers.findById(1L)).thenReturn(Optional.of(new Customer()));
        VehicleType type = new VehicleType();
        type.setId(3L);
        type.setPricePerHour(new BigDecimal("100000"));
        when(types.findById(3L)).thenReturn(Optional.of(type));
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(9L);
        terms.setDepositRate(new BigDecimal("0.5"));
        when(templates.getActiveTemplate()).thenReturn(terms);
        Vehicle car = new Vehicle();
        car.setId(1L);
        car.setStatus(Vehicle.STATUS_AVAILABLE);
        when(vehicles.findByVehicleTypeIdWithDetails(3L)).thenReturn(List.of(car));
        when(slots.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookings.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employees.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
    }

    @Test
    void aValidVoucherTakesTenPercentOffAndIsUsedUp() {
        Promotion voucher = voucher(1, TODAY, TODAY.plusDays(89));

        Booking booking = book("bu42-k7qm2x");

        // 10 hours x 100,000 = 1,000,000; 10% off
        assertThat(booking.getDiscountAmount()).isEqualByComparingTo("100000");
        assertThat(booking.getPromotion()).isSameAs(voucher);
        assertThat(voucher.getQuantity()).isZero();
    }

    @Test
    void aUsedUpVoucherGivesNoDiscount() {
        voucher(0, TODAY, TODAY.plusDays(89));

        Booking booking = book("BU42-K7QM2X");

        assertThat(booking.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(booking.getPromotion()).isNull();
    }

    @Test
    void anExpiredVoucherGivesNoDiscountAndKeepsItsUse() {
        Promotion voucher = voucher(1, TODAY.minusDays(90), TODAY.minusDays(1));

        Booking booking = book("BU42-K7QM2X");

        assertThat(booking.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(voucher.getQuantity()).isEqualTo(1);
    }

    private Promotion voucher(int uses, LocalDate start, LocalDate end) {
        Promotion voucher = new Promotion();
        voucher.setCode("BU42-K7QM2X");
        voucher.setDiscountType("percent");
        voucher.setDiscountValue(new BigDecimal("10"));
        voucher.setQuantity(uses);
        voucher.setStartDate(start);
        voucher.setEndDate(end);
        when(promotions.findByCodeForUpdate(any())).thenReturn(Optional.of(voucher));
        return voucher;
    }

    private Booking book(String code) {
        BookingCreateDTO dto = new BookingCreateDTO();
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS).withHour(9);
        dto.setStartTime(start);
        dto.setEndTime(start.plusHours(10));
        dto.setVehicleTypeId(3L);
        dto.setContractTemplateId(9L);
        dto.setAcceptedTerms(true);
        dto.setPromotionCode(code);
        return service.createBooking(1L, dto, "127.0.0.1");
    }
}
