package com.crms.mvc.controller;

import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.TrafficFineFormDTO;
import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.TrafficFineService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminTrafficFineController.class)
@Import(PassThroughSessionAccountService.class)
class AdminTrafficFineControllerTest {
    @MockitoBean private LicenseDocumentService licenseDocumentService; // the global advice reads it on every page
    @Autowired private MockMvc mvc;
    @MockitoBean private TrafficFineService trafficFineService;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;

    private MockHttpSession staff;

    @BeforeEach
    void setUp() {
        staff = TestSessions.staff(2L);
    }

    @Test
    void theTabListsRecordedFinesWithTheRentalFacts() throws Exception {
        when(trafficFineService.findAll()).thenReturn(List.of(new TrafficFineNoticeDTO(5L, 42L, "Nguyễn Văn A", "a@example.com",
                "51A-123.45", OffsetDateTime.parse("2026-10-20T14:30+07:00"), OffsetDateTime.parse("2026-10-25T10:00+07:00"),
                "Nhân viên", OffsetDateTime.parse("2026-10-20T09:00+07:00"), OffsetDateTime.parse("2026-10-20T18:00+07:00"), "CTR-0001")));

        mvc.perform(get("/admin/traffic-fines").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/traffic-fines/list/index"))
                .andExpect(content().string(containsString("51A-123.45")))
                .andExpect(content().string(containsString("CTR-0001")))
                .andExpect(content().string(containsString("20/10/2026 09:00")))
                .andExpect(content().string(containsString("/admin/traffic-fines/5/image")));
    }

    @Test
    void aLookupThatFindsARentalShowsItWithTheFormToRecordTheFine() throws Exception {
        Booking rental = new Booking();
        rental.setId(42L);
        when(trafficFineService.findBookingForViolation("51A-123.45", LocalDateTime.of(2026, 10, 20, 14, 30))).thenReturn(rental);
        BookingDetailDTO detail = new BookingDetailDTO();
        detail.setId(42L);
        detail.setCustomerName("Nguyễn Văn A");
        detail.setVehicleTypeName("Toyota Vios");
        detail.setLicensePlate("51A-123.45");
        detail.setPickupTime(OffsetDateTime.parse("2026-10-20T09:00+07:00"));
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(detail);

        mvc.perform(get("/admin/traffic-fines").session(staff)
                        .param("plate", "51A-123.45").param("violationAt", "2026-10-20T14:30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nguyễn Văn A")))
                .andExpect(content().string(containsString("name=\"image\"")));
    }

    @Test
    void whenNoRentalMatchesTheStaffIsToldWhyAndThereIsNoForm() throws Exception {
        when(trafficFineService.findBookingForViolation(any(), any()))
                .thenThrow(new NoSuchElementException("Không có đơn thuê nào của xe này tại thời điểm vi phạm"));

        mvc.perform(get("/admin/traffic-fines").session(staff)
                        .param("plate", "51A-123.45").param("violationAt", "2026-10-20T14:30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có đơn thuê nào của xe này tại thời điểm vi phạm")))
                .andExpect(content().string(not(containsString("name=\"image\""))));
    }

    @Test
    void recordingAFineSendsTheFormAndTheStaffId() throws Exception {
        mvc.perform(multipart("/admin/traffic-fines").file(new MockMultipartFile("image", "fine.jpg", "image/jpeg", new byte[]{1}))
                        .session(staff)
                        .param("bookingId", "42").param("plate", "51A-123.45").param("violationAt", "2026-10-20T14:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/traffic-fines"))
                .andExpect(flash().attributeExists("success"));

        verify(trafficFineService).record(any(TrafficFineFormDTO.class), eq(2L));
    }

    @Test
    void aRecordWithoutAViolationTimeIsNotSaved() throws Exception {
        mvc.perform(multipart("/admin/traffic-fines").file(new MockMultipartFile("image", "fine.jpg", "image/jpeg", new byte[]{1}))
                        .session(staff)
                        .param("bookingId", "42").param("plate", "51A-123.45"))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/traffic-fines/list/index"));

        verify(trafficFineService, never()).record(any(), any());
    }

    @Test
    void staffCanDeleteAMistakenRecord() throws Exception {
        mvc.perform(post("/admin/traffic-fines/5/delete").session(staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/traffic-fines"))
                .andExpect(flash().attributeExists("success"));

        verify(trafficFineService).delete(5L);
    }
}
