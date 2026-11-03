package com.crms.mvc.controller;

import com.crms.mvc.entity.Promotion;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.PromotionService;
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

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleTypeRepository vehicleTypeRepository;
    @MockitoBean private PromotionService promotionService;

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
                .andExpect(content().string(not(containsString("images.unsplash.com"))));
    }
}
