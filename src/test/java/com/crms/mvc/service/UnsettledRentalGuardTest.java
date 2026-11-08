package com.crms.mvc.service;

import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A customer who gave a car back but has not settled it cannot book again until the fees are settled. */
class UnsettledRentalGuardTest {

    @Test
    void aNewBookingIsRefusedWhileAReturnedRentalIsNotSettled() {
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ContractTemplateService templates = mock(ContractTemplateService.class);
        BookingServiceImpl service = new BookingServiceImpl(bookings, customers, mock(SlotRepository.class),
                mock(VehicleTypeRepository.class), mock(VehicleRepository.class), mock(EmployeeRepository.class),
                mock(PromotionRepository.class), mock(PaymentService.class), mock(ContractService.class), templates,
                mock(BookingFeeService.class), mock(PricingService.class));
        when(customers.findById(1L)).thenReturn(Optional.of(new Customer()));
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(9L);
        when(templates.getActiveTemplate()).thenReturn(terms);
        Booking returned = new Booking();
        returned.setId(31L);
        when(bookings.findReturnedUnsettledOfCustomer(1L)).thenReturn(List.of(returned));

        BookingCreateDTO dto = new BookingCreateDTO();
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS).withHour(9);
        dto.setStartTime(start);
        dto.setEndTime(start.plusHours(5));
        dto.setVehicleTypeId(3L);
        dto.setContractTemplateId(9L);
        dto.setAcceptedTerms(true);

        assertThatThrownBy(() -> service.createBooking(1L, dto, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("#31")
                .hasMessageContaining("chưa quyết toán");
        verify(bookings, never()).save(any());
    }
}
