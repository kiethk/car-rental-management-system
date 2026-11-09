package com.crms.mvc.controller;

import com.crms.mvc.service.PromotionService;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    private static final Map<String, String> FALLBACK_VEHICLE_IMAGES = createFallbackVehicleImages();

    private final VehicleTypeService vehicleTypeService;
    private final PromotionService promotionService;
    private final ReviewService reviewService;

    public HomeController(VehicleTypeService vehicleTypeService, PromotionService promotionService,
                          ReviewService reviewService) {
        this.vehicleTypeService = vehicleTypeService;
        this.promotionService = promotionService;
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1).toString());
        model.addAttribute("featuredVehicles", vehicleTypeService.findAll());
        model.addAttribute("ratings", reviewService.ratingsByVehicleType());
        var promotions = promotionService.findActiveWithImages();
        model.addAttribute("promotions", promotions);
        model.addAttribute("hasPromotionImages", !promotions.isEmpty());
        model.addAttribute("fallbackVehicleImages", FALLBACK_VEHICLE_IMAGES);
        return "home";
    }

    private static Map<String, String> createFallbackVehicleImages() {
        Map<String, String> images = new LinkedHashMap<>();
        images.put("Toyota Vios", "/images/vehicles/vios.jpg");
        images.put("VinFast VF e34", "/images/vehicles/vfe34.jpg");
        images.put("VinFast VF8", "/images/vehicles/vf8.jpg");
        images.put("VinFast VF9", "/images/vehicles/vf9.jpg");
        images.put("Mitsubishi Xpander", "/images/vehicles/xpander.jpg");
        images.put("Honda CR-V", "/images/vehicles/crv.jpg");
        images.put("Hyundai Santa Fe", "/images/vehicles/santafe.jpg");
        images.put("Mazda 3", "/images/vehicles/mazda3.jpg");
        images.put("Ford Ranger", "/images/vehicles/ranger.jpg");
        images.put("Kia Carnival", "/images/vehicles/carnival.jpg");
        return Collections.unmodifiableMap(images);
    }
}
