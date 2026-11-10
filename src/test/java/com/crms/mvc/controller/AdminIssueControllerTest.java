package com.crms.mvc.controller;

import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.support.PassThroughSessionAccountService;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.TestSessions;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.enums.IssueStatus;
import com.crms.mvc.enums.IssueType;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

@WebMvcTest(AdminIssueController.class)
@Import(PassThroughSessionAccountService.class)
class AdminIssueControllerTest {
    @MockitoBean private LicenseDocumentService licenseDocumentService; // the global advice reads it on every page
    @Autowired private MockMvc mvc;
    @MockitoBean private IssueService issueService;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;

    private MockHttpSession staff;

    @BeforeEach
    void setUp() {
        staff = TestSessions.staff(2L);
    }

    @Test
    void staffRecordsAnIssueAndLandsOnItsPage() throws Exception {
        Issue saved = new Issue();
        saved.setId(7L);
        when(issueService.create(any(IssueFormDTO.class), any(IssueActor.class))).thenReturn(saved);

        mvc.perform(validForm(post("/admin/issues")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"))
                .andExpect(flash().attributeExists("success"));

        ArgumentCaptor<IssueFormDTO> form = ArgumentCaptor.forClass(IssueFormDTO.class);
        ArgumentCaptor<IssueActor> actor = ArgumentCaptor.forClass(IssueActor.class);
        verify(issueService).create(form.capture(), actor.capture());
        assertEquals(42L, form.getValue().getBookingId());
        assertEquals(IssueType.DAMAGE, form.getValue().getIssueType());
        assertEquals(new BigDecimal("200000"), form.getValue().getFeeAmount());
        assertEquals(2L, actor.getValue().employeeId());
        assertEquals("staff", actor.getValue().role());
    }

    @Test
    void anIncompleteFormIsShownAgainWithoutCallingTheService() throws Exception {
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking());

        mvc.perform(post("/admin/issues").session(staff).param("bookingId", "42").param("issueType", "DAMAGE"))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/issues/form/index"));

        verify(issueService, never()).create(any(), any());
    }

    @Test
    void aBusinessRuleFailureComesBackAsAMessageOnTheForm() throws Exception {
        when(issueService.create(any(), any())).thenThrow(new IllegalStateException("not picked up yet"));

        mvc.perform(validForm(post("/admin/issues")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/new?bookingId=42"))
                .andExpect(flash().attribute("error", "not picked up yet"));
    }

    @Test
    void theNewFormIsOpenedForOneBooking() throws Exception {
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking());

        mvc.perform(get("/admin/issues/new").session(staff).param("bookingId", "42"))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/issues/form/index"))
                .andExpect(content().string(containsString("Nguyễn Văn An")));
    }

    @Test
    void theListShowsEveryIssue() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(issue()), PageRequest.of(0, 10), 1));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/issues/list/index"))
                .andExpect(content().string(containsString("Scratch on the bumper")))
                .andExpect(content().string(containsString("Hư hỏng")));
    }

    @Test
    void anIssuePageShowsItsDetailsAndFee() throws Exception {
        when(issueService.findDetail(7L)).thenReturn(Optional.of(issue()));

        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/issues/detail/index"))
                .andExpect(content().string(containsString("Scratch on the bumper")))
                .andExpect(content().string(containsString("200")));
    }

    @Test
    void directSettlementFromTheIssuePagePassesMethodAndNote() throws Exception {
        mvc.perform(post("/admin/issues/7/settle-direct").session(staff)
                        .param("paymentMethod", "CASH").param("note", "Customer paid cash"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"))
                .andExpect(flash().attributeExists("success"));

        ArgumentCaptor<IssueActor> actor = ArgumentCaptor.forClass(IssueActor.class);
        verify(issueService).settleDirectly(eq(7L), eq("CASH"), eq("Customer paid cash"), actor.capture());
        assertEquals("staff", actor.getValue().role());
    }

    @Test
    void cancellingFromTheIssuePagePassesTheReason() throws Exception {
        mvc.perform(post("/admin/issues/7/cancel").session(staff).param("reason", "Wrong booking"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"));

        verify(issueService).cancel(eq(7L), eq("Wrong booking"), any(IssueActor.class));
    }

    @Test
    void aRuleTheServiceEnforcesComesBackAsAMessageNotAnErrorPage() throws Exception {
        when(issueService.settleDirectly(eq(7L), any(), any(), any(IssueActor.class)))
                .thenThrow(new IllegalStateException("Sự cố không thể thu tiền"));

        mvc.perform(post("/admin/issues/7/settle-direct").session(staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"))
                .andExpect(flash().attribute("error", "Sự cố không thể thu tiền"));
    }

    @Test
    void anOpenIssueOffersDirectSettleAndCancel() throws Exception {
        when(issueService.findDetail(7L)).thenReturn(Optional.of(issue()));

        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(content().string(containsString("Thu tiền tại quầy")))
                .andExpect(content().string(containsString("Huỷ sự cố")));
    }

    @Test
    void aResolvedIssueOffersNoChanges() throws Exception {
        IssueDTO resolved = issue();
        resolved.setStatus(IssueStatus.RESOLVED);
        resolved.setFeePaid(true);
        when(issueService.findDetail(7L)).thenReturn(Optional.of(resolved));

        mvc.perform(get("/admin/issues/7").session(admin()))
                .andExpect(content().string(not(containsString("Thu tiền tại quầy"))))
                .andExpect(content().string(not(containsString("Huỷ sự cố"))));
        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(content().string(not(containsString("Thu tiền tại quầy"))))
                .andExpect(content().string(not(containsString("Huỷ sự cố"))));
    }

    @Test
    void staffUploadsOnePhotoAtATime() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1});

        mvc.perform(multipart("/admin/issues/7/evidences").file(photo).session(staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"))
                .andExpect(flash().attributeExists("success"));

        verify(issueService).addEvidence(eq(7L), any());
    }

    @Test
    void aRejectedPhotoComesBackAsAMessage() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1});
        when(issueService.addEvidence(eq(7L), any())).thenThrow(new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed"));

        mvc.perform(multipart("/admin/issues/7/evidences").file(photo).session(staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Only JPEG, PNG, and WebP images are allowed"));
    }

    @Test
    void aPhotoCanBeRemovedFromTheIssuePage() throws Exception {
        mvc.perform(post("/admin/issues/7/evidences/3/delete").session(staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/issues/7"));

        verify(issueService).removeEvidence(7L, 3L);
    }

    @Test
    void aPhotoIsServedWithItsImageType() throws Exception {
        when(issueService.loadEvidence(7L, 3L)).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "issue-1.png";
            }
        });

        mvc.perform(get("/admin/issues/7/evidences/3").session(staff))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void theIssuePageShowsItsPhotosWithRemoveButtonsOnlyWhileOpen() throws Exception {
        IssueDTO open = issue();
        open.setEvidences(List.of(new IssueDTO.EvidenceDTO(3L, "issue-1.png", OffsetDateTime.now())));
        when(issueService.findDetail(7L)).thenReturn(Optional.of(open));

        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(content().string(containsString("/admin/issues/7/evidences/3")))
                .andExpect(content().string(containsString("/admin/issues/7/evidences/3/delete")))
                .andExpect(content().string(containsString("Thêm ảnh")));

        IssueDTO resolved = issue();
        resolved.setStatus(IssueStatus.RESOLVED);
        resolved.setEvidences(open.getEvidences());
        when(issueService.findDetail(7L)).thenReturn(Optional.of(resolved));

        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(content().string(containsString("/admin/issues/7/evidences/3")))
                .andExpect(content().string(not(containsString("/admin/issues/7/evidences/3/delete"))))
                .andExpect(content().string(containsString("Thêm ảnh")));
    }

    @Test
    void noPhotoCanBeAddedWhenTheFeeIsBeingPaid() throws Exception {
        IssueDTO locked = issue();
        locked.setStatus(IssueStatus.RESOLVED);
        locked.setFeeLocked(true);
        when(issueService.findDetail(7L)).thenReturn(Optional.of(locked));

        mvc.perform(get("/admin/issues/7").session(staff))
                .andExpect(content().string(not(containsString("Thêm ảnh"))));
    }

    @Test
    void theListNoLongerOffersTheTrafficFineLookup() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff))
                .andExpect(content().string(not(containsString("/admin/issues/traffic-fine"))))
                .andExpect(content().string(not(containsString("Tra đơn thuê"))));
    }

    @Test
    void theListShowsOnlyTheOpenCountOnTop() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(issue()), PageRequest.of(0, 10), 1));
        when(issueService.countOpen()).thenReturn(4L);

        mvc.perform(get("/admin/issues").session(staff))
                .andExpect(content().string(containsString("Đang mở")))
                .andExpect(content().string(not(containsString("Chờ khách thanh toán"))))
                .andExpect(content().string(not(containsString("Đã thu tháng này"))));
    }

    @Test
    void theFiltersReachTheServiceAndStayInTheForm() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff)
                        .param("status", "resolved").param("type", "DAMAGE").param("plate", "51A")
                        .param("bookingId", "42").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"51A\"")))
                .andExpect(content().string(containsString("value=\"42\"")))
                .andExpect(content().string(containsString("value=\"2026-10-01\"")))
                .andExpect(content().string(containsString("value=\"2026-10-31\"")))
                .andExpect(content().string(containsString("<option value=\"resolved\" selected=\"selected\"")))
                .andExpect(content().string(containsString("<option value=\"DAMAGE\" selected=\"selected\"")));

        ArgumentCaptor<IssueQueryDTO> query = ArgumentCaptor.forClass(IssueQueryDTO.class);
        verify(issueService).searchForAdmin(query.capture());
        assertEquals(IssueStatus.RESOLVED, query.getValue().getIssueStatus());
        assertEquals(IssueType.DAMAGE, query.getValue().getIssueType());
        assertEquals("51A", query.getValue().getPlate());
        assertEquals(42L, query.getValue().getBookingId());
        assertEquals(java.time.LocalDate.of(2026, 10, 1), query.getValue().getFrom());
    }

    @Test
    void aBadFilterValueDoesNotBreakThePage() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff).param("from", "not-a-date").param("bookingId", "abc"))
                .andExpect(status().isOk());
    }

    @Test
    void aResolvedIssueWhoseFeeIsUnpaidCarriesAnOwedBadge() throws Exception {
        IssueDTO owed = issue();
        owed.setStatus(IssueStatus.RESOLVED);
        owed.setFeePaid(false);
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(owed), PageRequest.of(0, 10), 1));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff))
                .andExpect(content().string(containsString("Chưa thanh toán")));
    }

    @Test
    void aPaidOrOpenIssueCarriesNoOwedBadge() throws Exception {
        IssueDTO paid = issue();
        paid.setStatus(IssueStatus.RESOLVED);
        paid.setFeePaid(true);
        IssueDTO open = issue();
        open.setId(8L);
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(paid, open), PageRequest.of(0, 10), 2));
        when(issueService.countOpen()).thenReturn(1L);

        mvc.perform(get("/admin/issues").session(staff))
                .andExpect(content().string(not(containsString("Chưa thanh toán"))));
    }

    @Test
    void thePagerKeepsTheFilters() throws Exception {
        when(issueService.searchForAdmin(any())).thenReturn(new PageImpl<>(
                List.of(issue()), PageRequest.of(0, 10), 25));
        when(issueService.countOpen()).thenReturn(0L);

        mvc.perform(get("/admin/issues").session(staff).param("status", "open").param("plate", "51A"))
                .andExpect(content().string(containsString("page=2")))
                .andExpect(content().string(containsString("status=open")))
                .andExpect(content().string(containsString("plate=51A")));
    }

    private static MockHttpSession admin() {
        return TestSessions.admin(1L);
    }

    private MockHttpServletRequestBuilder validForm(MockHttpServletRequestBuilder request) {
        return request.session(staff)
                .param("bookingId", "42").param("issueType", "DAMAGE")
                .param("subject", "Scratch on the bumper").param("description", "About 15 cm.")
                .param("occurredAt", "2026-10-30T14:30").param("feeAmount", "200000");
    }

    private BookingDetailDTO booking() {
        BookingDetailDTO booking = new BookingDetailDTO();
        booking.setId(42L);
        booking.setCustomerName("Nguyễn Văn An");
        booking.setVehicleTypeName("Vios");
        booking.setLicensePlate("51A-123.45");
        booking.setStatus("active");
        return booking;
    }

    private IssueDTO issue() {
        IssueDTO dto = new IssueDTO();
        dto.setId(7L);
        dto.setBookingId(42L);
        dto.setCustomerName("Nguyễn Văn An");
        dto.setVehicleId(9L);
        dto.setLicensePlate("51A-123.45");
        dto.setVehicleName("Vios");
        dto.setReporterName("Trần Thị Bình");
        dto.setIssueType(IssueType.DAMAGE);
        dto.setStatus(IssueStatus.OPEN);
        dto.setSubject("Scratch on the bumper");
        dto.setDescription("About 15 cm.");
        dto.setOccurredAt(OffsetDateTime.now().minusHours(3));
        dto.setReportedAt(OffsetDateTime.now().minusHours(2));
        dto.setFeeAmount(new BigDecimal("200000"));
        return dto;
    }
}
