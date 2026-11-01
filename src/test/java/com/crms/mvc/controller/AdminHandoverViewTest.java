package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.HandoverRecordDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.HandoverService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AdminBookingController.class)
class AdminHandoverViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private HandoverService handoverService;

    @BeforeEach
    void setUp() {
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());
        when(paymentService.getPendingRefund(42L)).thenReturn(Optional.empty());
    }

    @Test
    void assignedStaffSeesHandoverForm() throws Exception {
        stubBooking("confirmed", new HandoverSummaryDTO());

        mvc.perform(get("/admin/bookings/42").session(TestSessions.staff(2L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hand Over Vehicle")))
                .andExpect(content().string(containsString("name=\"licenseImage\"")))
                .andExpect(content().string(containsString("name=\"idCardImage\"")))
                .andExpect(content().string(not(containsString("assign-staff"))));
    }

    @Test
    void otherStaffOnlySeesWarning() throws Exception {
        stubBooking("confirmed", new HandoverSummaryDTO());

        mvc.perform(get("/admin/bookings/42").session(TestSessions.staff(3L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Only that staff member or an admin")))
                .andExpect(content().string(not(containsString("name=\"licenseImage\""))));
    }

    @Test
    void adminCanChangeAssignedStaff() throws Exception {
        stubBooking("confirmed", new HandoverSummaryDTO());
        Employee staff = new Employee();
        staff.setId(2L);
        staff.setName("Support Staff");
        staff.setRole("staff");
        when(handoverService.findAssignableEmployees()).thenReturn(List.of(staff));

        mvc.perform(get("/admin/bookings/42").session(TestSessions.admin(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Support Staff (staff)")))
                .andExpect(content().string(containsString("name=\"licenseImage\"")));
    }

    @Test
    void activeBookingShowsReturnFormAndRecords() throws Exception {
        HandoverSummaryDTO summary = new HandoverSummaryDTO();
        HandoverRecordDTO record = new HandoverRecordDTO();
        record.setId(7L);
        record.setType("CHECK_IN");
        record.setSubject("Handover: ODO 1200 km, fuel 80%");
        record.setEmployeeName("Support Staff");
        record.setReportedAt(OffsetDateTime.now());
        record.setEvidence(true);
        record.setSecondaryEvidence(true);
        summary.setRecords(List.of(record));
        stubBooking("active", summary);

        mvc.perform(get("/admin/bookings/42").session(TestSessions.staff(2L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Record Vehicle Return")))
                .andExpect(content().string(containsString("surcharges[0].amount")))
                .andExpect(content().string(containsString("Handover: ODO 1200 km, fuel 80%")))
                .andExpect(content().string(containsString("/admin/bookings/42/records/7/evidence?secondary=true")));
    }

    @Test
    void returnedBookingAsksForPaymentMethodWhenCustomerOwes() throws Exception {
        HandoverSummaryDTO summary = settlementSummary("700000");
        stubBooking("active", summary);

        mvc.perform(get("/admin/bookings/42").session(TestSessions.staff(2L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Complete Settlement")))
                .andExpect(content().string(containsString("Customer pays")))
                .andExpect(content().string(containsString("name=\"paymentMethod\"")))
                .andExpect(content().string(not(containsString("name=\"bankCode\""))))
                .andExpect(content().string(not(containsString("Record Vehicle Return"))));
    }

    @Test
    void returnedBookingAsksForBankDetailsWhenRefundIsDue() throws Exception {
        stubBooking("active", settlementSummary("-200000"));

        mvc.perform(get("/admin/bookings/42").session(TestSessions.staff(2L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Refund to customer")))
                .andExpect(content().string(containsString("name=\"bankCode\"")))
                .andExpect(content().string(not(containsString("name=\"paymentMethod\""))));
    }

    @Test
    void invalidCheckInNeverReachesService() throws Exception {
        mvc.perform(multipart("/admin/bookings/42/check-in")
                        .file(new MockMultipartFile("licenseImage", "l.jpg", "image/jpeg", new byte[]{1}))
                        .param("odometer", "1200").param("fuelPercent", "150")
                        .session(TestSessions.staff(2L)))
                .andExpect(redirectedUrl("/admin/bookings/42"))
                .andExpect(flash().attributeExists("error"));

        verify(handoverService, never()).checkIn(any(), any(), any());
    }

    @Test
    void validCheckInPassesLoggedInEmployeeToService() throws Exception {
        mvc.perform(multipart("/admin/bookings/42/check-in")
                        .file(new MockMultipartFile("licenseImage", "l.jpg", "image/jpeg", new byte[]{1}))
                        .file(new MockMultipartFile("idCardImage", "i.jpg", "image/jpeg", new byte[]{1}))
                        .param("odometer", "1200").param("fuelPercent", "80").param("documentsVerified", "true")
                        .session(TestSessions.staff(2L)))
                .andExpect(redirectedUrl("/admin/bookings/42"))
                .andExpect(flash().attributeExists("success"));

        verify(handoverService).checkIn(eq(42L), eq(2L), any(CheckInDTO.class));
    }

    @Test
    void serviceRefusalIsShownToOtherStaff() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("Only the assigned staff member or an admin can perform this action"))
                .when(handoverService).settle(eq(42L), eq(3L), any());

        mvc.perform(post("/admin/bookings/42/settle").session(TestSessions.staff(3L)))
                .andExpect(flash().attribute("error", containsString("Only the assigned staff member")));
    }

    @Test
    void staffCannotChangeAssignedStaff() throws Exception {
        mvc.perform(post("/admin/bookings/42/assign-staff").param("staffId", "3").session(TestSessions.staff(2L)))
                .andExpect(flash().attributeExists("error"));

        verify(handoverService, never()).reassignStaff(any(), any());
    }

    @Test
    void missingEvidenceReturns404() throws Exception {
        when(handoverService.loadEvidence(42L, 7L, false)).thenReturn(null);

        mvc.perform(get("/admin/bookings/42/records/7/evidence").session(TestSessions.staff(2L)))
                .andExpect(status().isNotFound());
    }

    private HandoverSummaryDTO settlementSummary(String finalAmount) {
        HandoverSummaryDTO summary = new HandoverSummaryDTO();
        summary.setReturned(true);
        summary.setRentalTotal(new BigDecimal("1000000"));
        summary.setDepositAmount(new BigDecimal("300000"));
        summary.setFinalAmount(new BigDecimal(finalAmount));
        return summary;
    }

    private void stubBooking(String status, HandoverSummaryDTO summary) {
        BookingDetailDTO detail = new BookingDetailDTO();
        OffsetDateTime now = OffsetDateTime.now();
        detail.setId(42L);
        detail.setStatus(status);
        detail.setVehicleId(5L);
        detail.setVehicleTypeId(1L);
        detail.setVehicleTypeName("Toyota Vios");
        detail.setCustomerName("Nguyen Van A");
        detail.setStaffId(2L);
        detail.setStaffName("Support Staff");
        detail.setPickupTime(now);
        detail.setScheduledEndTime(now.plusDays(1));
        detail.setCreatedAt(now);
        detail.setTotalPrice(new BigDecimal("1000000"));
        detail.setDepositAmount(new BigDecimal("300000"));
        detail.setDiscountAmount(BigDecimal.ZERO);
        detail.setPricePerHour(new BigDecimal("100000"));
        detail.setLateFee(BigDecimal.ZERO);
        detail.setPayments(List.of());
        detail.setFees(List.of());
        when(bookingService.getBookingDetailForAdmin(42L)).thenReturn(detail);
        when(handoverService.getSummary(42L)).thenReturn(summary);
    }
}
