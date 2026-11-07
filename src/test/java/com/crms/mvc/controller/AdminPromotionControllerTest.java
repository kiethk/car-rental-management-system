package com.crms.mvc.controller;

import com.crms.mvc.entity.Promotion;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.PromotionService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.util.CurrentAccount;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AdminPromotionController.class)
class AdminPromotionControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PromotionService promotionService;

    @Test
    void promotionListRendersImageAndEditAction() throws Exception {
        when(promotionService.findAll()).thenReturn(List.of(promotion()));

        mvc.perform(get("/admin/promotions").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/uploads/promotions/welcome.png")))
                .andExpect(content().string(containsString("WELCOME10")))
                .andExpect(content().string(containsString("/admin/promotions/4/edit")));
    }

    @Test
    void editFormShowsCurrentImageAndMultipartUpload() throws Exception {
        when(promotionService.findById(4L)).thenReturn(Optional.of(promotion()));

        mvc.perform(get("/admin/promotions/4/edit").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("name=\"imageFile\"")))
                .andExpect(content().string(containsString("/uploads/promotions/welcome.png")));
    }

    @Test
    void updateSavesEditedPromotion() throws Exception {
        Promotion updated = promotion();
        updated.setCode("SPRING20");
        when(promotionService.update(org.mockito.ArgumentMatchers.eq(4L), any())).thenReturn(updated);

        mvc.perform(post("/admin/promotions/4").session(adminSession())
                        .param("id", "4")
                        .param("code", "SPRING20")
                        .param("discountType", "percent")
                        .param("discountValue", "20")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(10).toString())
                        .param("quantity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promotions"))
                .andExpect(flash().attribute("success", "Promotion SPRING20 updated successfully."));

        verify(promotionService).update(org.mockito.ArgumentMatchers.eq(4L), any());
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentAccount.SESSION_KEY,
                new SessionAccount(1L, SessionAccount.EMPLOYEE, "Admin Manager", "admin@crms.test", "admin", null));
        return session;
    }

    private Promotion promotion() {
        Promotion promotion = new Promotion();
        promotion.setId(4L);
        promotion.setCode("WELCOME10");
        promotion.setDiscountType("percent");
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setStartDate(LocalDate.now().minusDays(1));
        promotion.setEndDate(LocalDate.now().plusDays(10));
        promotion.setQuantity(25);
        promotion.setUrlImagePromotion("/uploads/promotions/welcome.png");
        return promotion;
    }
}
