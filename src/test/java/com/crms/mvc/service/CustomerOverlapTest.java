package com.crms.mvc.service;

import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** One customer cannot hold two paid or ongoing rentals at the same time. */
class CustomerOverlapTest {

    @Test
    void aSecondBookingOverlappingAPaidOneIsRefused() {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        VehicleTypeRepository types = mock(VehicleTypeRepository.class);
        ContractTemplateService templates = mock(ContractTemplateService.class);
        BookingServiceImpl service = new BookingServiceImpl(bookings, customers, mock(SlotRepository.class), types,
                mock(VehicleRepository.class), mock(EmployeeRepository.class), mock(PromotionRepository.class),
                mock(PaymentService.class), mock(ContractService.class), templates, mock(BookingFeeService.class),
                mock(PricingService.class));

        when(customers.findById(1L)).thenReturn(Optional.of(new Customer()));
        VehicleType type = new VehicleType();
        type.setId(3L);
        when(types.findById(3L)).thenReturn(Optional.of(type));
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(9L);
        when(templates.getActiveTemplate()).thenReturn(terms);
        Booking existing = new Booking();
        existing.setId(77L);
        when(bookings.findPaidBookingsByCustomerAndTimeRange(eq(1L), any(), any())).thenReturn(List.of(existing));

        BookingCreateDTO dto = new BookingCreateDTO();
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS).withHour(9);
        dto.setStartTime(start);
        dto.setEndTime(start.plusHours(5));
        dto.setVehicleTypeId(3L);
        dto.setContractTemplateId(9L);
        dto.setAcceptedTerms(true);

        assertThatThrownBy(() -> service.createBooking(1L, dto, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bạn đã có đơn #77");
        verify(bookings, never()).save(any());
    }
}
