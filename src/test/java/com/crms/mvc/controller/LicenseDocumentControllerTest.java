package com.crms.mvc.controller;

import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(LicenseDocumentController.class)
class LicenseDocumentControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private LicenseDocumentService licenseDocumentService;
    @MockitoBean private FileUploadService fileUploadService;

    @Test
    void customerCanUploadAgainAfterHavingALicense() throws Exception {
        when(licenseDocumentService.submitLicense(eq(1L), any())).thenReturn(true);
        MockHttpSession session = TestSessions.customer(1L);

        uploadLicense(session);
        uploadLicense(session);

        verify(licenseDocumentService, times(2)).submitLicense(eq(1L), any());
    }

    @Test
    void existingLicenseStillShowsReplaceForm() throws Exception {
        when(licenseDocumentService.getCustomerLicense(1L)).thenReturn(license(LocalDate.now().plusYears(2)));

        mvc.perform(get("/licenses/my-license").session(TestSessions.customer(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cập nhật giấy phép lái xe")))
                .andExpect(content().string(containsString("Đã tải lên")))
                .andExpect(content().string(not(containsString("Pending"))));
    }

    @Test
    void expiredLicenseIsFlagged() throws Exception {
        when(licenseDocumentService.getCustomerLicense(1L)).thenReturn(license(LocalDate.now().minusDays(1)));

        mvc.perform(get("/licenses/my-license").session(TestSessions.customer(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã hết hạn")));
    }

    @Test
    void reviewPagesNoLongerExist() throws Exception {
        mvc.perform(get("/admin/licenses").session(TestSessions.staff(2L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void employeeIsSentToDashboardFromCustomerLicensePage() throws Exception {
        mvc.perform(get("/licenses/my-license").session(TestSessions.staff(2L)))
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    private void uploadLicense(MockHttpSession session) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "licenseImage", "license.jpg", "image/jpeg", new byte[]{1, 2, 3});
        mvc.perform(multipart("/licenses").file(image).session(session)
                        .param("licenseNumber", "B2-12345678")
                        .param("licenseClass", "B2")
                        .param("licenseExpiry", LocalDate.now().plusYears(3).toString()))
                .andExpect(redirectedUrl("/licenses/my-license?submitted"));
    }

    private LicenseDocument license(LocalDate expiry) {
        Customer customer = new Customer();
        customer.setId(1L);
        LicenseDocument license = new LicenseDocument(customer, "B2-12345678", "B2", expiry,
                "license.jpg", OffsetDateTime.now());
        license.setId(10L);
        return license;
    }
}
