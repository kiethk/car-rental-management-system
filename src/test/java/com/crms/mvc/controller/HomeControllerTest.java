package com.crms.mvc.controller;

import com.crms.mvc.dto.RatingSummaryDTO;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.PromotionService;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.VehicleTypeService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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
    @MockitoBean private ReviewService reviewService;

    @Test
    void rendersPromotionImagesInTheHeroCarousel() throws Exception {
        Promotion promotion = new Promotion();
        promotion.setCode("WELCOME10");
        promotion.setUrlImagePromotion("/uploads/promotions/welcome.png");
        when(promotionService.findActiveWithImages()).thenReturn(List.of(promotion));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/uploads/promotions/welcome.png")))
                .andExpect(content().string(containsString("WELCOME10")))
                .andExpect(content().string(containsString("aria-label=\"Khuyến mãi hiện có\"")))
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
                .andExpect(content().string(containsString("aria-label=\"Xe cho thuê nổi bật\"")))
                .andExpect(content().string(not(containsString("images.unsplash.com"))));
    }

    @Test
    void featuredCarsShowRatingsFromReviewsNotMadeUpNumbers() throws Exception {
        VehicleType reviewed = new VehicleType();
        reviewed.setId(1L);
        reviewed.setName("Vios");
        reviewed.setBrand("Toyota");
        reviewed.setPricePerHour(new java.math.BigDecimal("150000"));
        VehicleType unreviewed = new VehicleType();
        unreviewed.setId(2L);
        unreviewed.setName("Accent");
        unreviewed.setBrand("Hyundai");
        unreviewed.setPricePerHour(new java.math.BigDecimal("140000"));
        when(vehicleTypeService.findAll()).thenReturn(List.of(reviewed, unreviewed));
        when(promotionService.findActiveWithImages()).thenReturn(List.of());
        when(reviewService.ratingsByVehicleType()).thenReturn(Map.of(1L, new RatingSummaryDTO(4.5, 2)));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4.5")))
                .andExpect(content().string(containsString("(2 đánh giá)")))
                .andExpect(content().string(containsString("Chưa có đánh giá")))
                .andExpect(content().string(not(containsString("124 đánh giá"))));
    }
}
