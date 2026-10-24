package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminBookingController.class)
class AdminBookingContractViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private VehicleRepository vehicleRepository;

    @Test
    void confirmedBookingWithoutPdfOffersRetry() throws Exception {
        when(bookingService.getBookingDetailForAdmin(42L)).thenReturn(confirmedBooking());
        when(vehicleRepository.findByStatus("available")).thenReturn(List.of());

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("employeeId", 2L);
        staff.setAttribute("employeeRole", "staff");

        mvc.perform(get("/admin/bookings/42").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings/manage"))
                .andExpect(content().string(containsString("Terms v1")))
                .andExpect(content().string(containsString("from IP 127.0.0.1")))
                .andExpect(content().string(containsString("Issue PDF and email")));
    }

    private BookingDetailDTO confirmedBooking() {
        OffsetDateTime now = OffsetDateTime.now();
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(1L);
        terms.setVersionNo(1);
        terms.setTitle("Hợp đồng thuê xe ô tô tự lái");
        terms.setTermsContent("1. Bàn giao xe đúng hẹn.");
        terms.setDepositRate(new BigDecimal("0.5000"));
        terms.setLateFeeMultiplier(new BigDecimal("2.00"));
        terms.setFreeCancellationHours(72);
        terms.setStatus(ContractTemplateStatus.ACTIVE);

        ContractDTO contract = new ContractDTO();
        contract.setContractNumber("CTR-20261023-0042");
        contract.setAcceptedAt(now.minusHours(1));
        contract.setAcceptedIp("127.0.0.1");
        contract.setIssued(false);
        contract.setTerms(terms);

        BookingDetailDTO booking = new BookingDetailDTO();
        booking.setId(42L);
        booking.setCustomerName("Nguyễn Văn An");
        booking.setCustomerPhone("0901234567");
        booking.setCustomerEmail("an@example.com");
        booking.setVehicleTypeId(1L);
        booking.setVehicleTypeName("Vios");
        booking.setVehicleCategory("sedan");
        booking.setSeats(5);
        booking.setLicensePlate("Unassigned");
        booking.setPickupTime(now.plusDays(1));
        booking.setScheduledEndTime(now.plusDays(1).plusHours(9));
        booking.setPricePerHour(new BigDecimal("150000"));
        booking.setTotalPrice(new BigDecimal("1350000"));
        booking.setDepositAmount(new BigDecimal("675000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setLateFee(BigDecimal.ZERO);
        booking.setStatus("confirmed");
        booking.setCreatedAt(now.minusHours(1));
        booking.setPayments(List.of());
        booking.setFees(List.of());
        booking.setContract(contract);
        return booking;
    }
}
