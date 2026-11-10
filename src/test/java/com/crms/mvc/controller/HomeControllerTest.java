package com.crms.mvc.controller;

import com.crms.mvc.entity.Promotion;
import com.crms.mvc.service.PromotionService;
import com.crms.mvc.service.VehicleTypeService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private PromotionService promotionService;

    @Test
    void licenseUploadAndAdminReviewRoutesAreGone() throws Exception {
        mvc.perform(get("/licenses/my-license").session(TestSessions.customer(1L)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/licenses").session(TestSessions.staff(2L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rendersPromotionImagesOnlyInGalleryAboveHowItWorks() throws Exception {
        Promotion promotion = new Promotion();
        promotion.setCode("WELCOME10");
        promotion.setUrlImagePromotion("/uploads/promotions/welcome.png");
        when(promotionService.findActiveWithImages()).thenReturn(List.of(promotion));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/images/vehicles/vios.jpg")))
                .andExpect(content().string(containsString("promotion-gallery__track")))
                .andExpect(content().string(containsString("/uploads/promotions/welcome.png")))
                .andExpect(content().string(containsString("Các chương trình khuyến mãi")))
                .andExpect(content().string(containsString("aria-label=\"Featured rental cars\"")))
                .andExpect(content().string(containsString("WELCOME10")))
                .andExpect(content().string(not(containsString("images.unsplash.com"))));
    }

    @Test
    void rotatesLocalVehicleImagesWhenThereAreNoPromotions() throws Exception {
        when(promotionService.findActiveWithImages()).thenReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/images/vehicles/vios.jpg")))
                .andExpect(content().string(containsString("/images/vehicles/vfe34.jpg")))
                .andExpect(content().string(containsString("Toyota Vios")))
                .andExpect(content().string(containsString("aria-label=\"Featured rental cars\"")))
                .andExpect(content().string(containsString("Không có khuyến mãi")))
                .andExpect(content().string(not(containsString("id=\"promotion-gallery-track\""))))
                .andExpect(content().string(not(containsString("images.unsplash.com"))));
    }
}
