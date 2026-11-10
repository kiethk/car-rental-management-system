package com.crms.mvc.service;

import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.VehicleAvailabilityServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The plate a customer picks at checkout is either the one they get, or the booking fails. */
class BookingPickedPlateTest {

    @Test
    void aPickedPlateTakenMeanwhileFailsTheBookingInsteadOfAssigningAnotherCar() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        VehicleTypeRepository vehicleTypeRepository = mock(VehicleTypeRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        ContractTemplateService contractTemplateService = mock(ContractTemplateService.class);
        BookingServiceImpl service = new BookingServiceImpl(bookingRepository, customerRepository, mock(SlotRepository.class), vehicleTypeRepository, vehicleRepository, mock(EmployeeRepository.class), mock(PromotionRepository.class), mock(PaymentService.class), mock(ContractService.class), contractTemplateService, mock(PricingService.class), new VehicleAvailabilityServiceImpl(bookingRepository, vehicleRepository, vehicleTypeRepository, contractTemplateService, mock(PricingService.class)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(new Customer()));
        VehicleType type = new VehicleType();
        type.setId(3L);
        when(vehicleTypeRepository.findById(3L)).thenReturn(Optional.of(type));
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(9L);
        when(contractTemplateService.getActiveTemplate()).thenReturn(terms);

        // Only car 1 is still free; the customer picked car 2, which someone else just booked.
        Vehicle stillFree = new Vehicle();
        stillFree.setId(1L);
        stillFree.setStatus(VehicleStatus.AVAILABLE);
        when(vehicleRepository.findByVehicleTypeIdWithDetails(3L)).thenReturn(List.of(stillFree));

        BookingCreateDTO dto = new BookingCreateDTO();
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS).withHour(9);
        dto.setStartTime(start);
        dto.setEndTime(start.plusHours(5));
        dto.setVehicleTypeId(3L);
        dto.setVehicleId(2L);
        dto.setContractTemplateId(9L);
        dto.setAcceptedTerms(true);

        assertThatThrownBy(() -> service.createBooking(1L, dto, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Xe bạn chọn vừa được đặt");
        verify(bookingRepository, never()).save(any());
    }
}
