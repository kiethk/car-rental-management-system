package com.crms.mvc.service;

import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingQuoteTest {
    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final VehicleTypeRepository types = mock(VehicleTypeRepository.class);
    private final ContractTemplateService contracts = mock(ContractTemplateService.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private BookingService service;
    private BookingCreateDTO request;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookings, mock(CustomerRepository.class), mock(SlotRepository.class),
                types, mock(VehicleRepository.class), mock(EmployeeRepository.class), promotions,
                mock(PaymentService.class), mock(ContractService.class), contracts,
                new PricingServiceImpl(), mock(VehicleAvailabilityService.class));
        VehicleType type = new VehicleType();
        type.setId(7L);
        type.setPricePerHour(new BigDecimal("100000"));
        when(types.findById(7L)).thenReturn(Optional.of(type));
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(4L);
        terms.setDepositRate(new BigDecimal("0.30"));
        when(contracts.getActiveTemplate()).thenReturn(terms);
        request = new BookingCreateDTO();
        request.setVehicleTypeId(7L);
        request.setContractTemplateId(4L);
        request.setStartTime(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0));
        request.setEndTime(request.getStartTime().plusHours(3));
    }

    @Test
    void previewsRealPromotionAndContractRateWithoutRedeemingOrCreatingBooking() {
        Promotion promotion = new Promotion();
        promotion.setDiscountType("percent");
        promotion.setDiscountValue(new BigDecimal("20"));
        promotion.setQuantity(1);
        promotion.setStartDate(LocalDate.now().minusDays(1));
        promotion.setEndDate(LocalDate.now().plusDays(1));
        request.setPromotionCode(" REAL20 ");
        when(promotions.findByCodeIgnoreCase("REAL20")).thenReturn(Optional.of(promotion));
        var quote = service.previewPrice(request);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("240000");
        assertThat(quote.getDepositAmount()).isEqualByComparingTo("72000");
        assertThat(promotion.getQuantity()).isEqualTo(1);
        verify(promotions, never()).save(any());
        verify(promotions, never()).findByCodeForUpdate(any());
        verifyNoInteractions(bookings);
    }

    @Test
    void rejectsUnknownOrExhaustedPromotion() {
        request.setPromotionCode("MISSING");
        when(promotions.findByCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.previewPrice(request)).isInstanceOf(IllegalArgumentException.class);
        Promotion exhausted = new Promotion();
        exhausted.setQuantity(0);
        when(promotions.findByCodeIgnoreCase("MISSING")).thenReturn(Optional.of(exhausted));
        assertThatThrownBy(() -> service.previewPrice(request)).isInstanceOf(IllegalArgumentException.class);
        verify(promotions, never()).save(any());
    }

    @Test
    void rejectsChangedContractInsteadOfShowingADifferentDeposit() {
        request.setContractTemplateId(3L);
        assertThatThrownBy(() -> service.previewPrice(request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Điều khoản");
    }

    @Test
    void previewWithoutPromotionUsesTheSameBasePriceAndRate() {
        var quote = service.previewPrice(request);
        assertThat(quote.getBaseRentalPrice()).isEqualByComparingTo("300000");
        assertThat(quote.getDepositAmount()).isEqualByComparingTo("90000");
        verifyNoInteractions(promotions, bookings);
    }
}
