package com.crms.mvc.controller;

import com.crms.mvc.service.PromotionService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final VehicleTypeService vehicleTypeService;
    private final PromotionService promotionService;

    public HomeController(VehicleTypeService vehicleTypeService, PromotionService promotionService) {
        this.vehicleTypeService = vehicleTypeService;
        this.promotionService = promotionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1).toString());
        model.addAttribute("featuredVehicles", vehicleTypeService.findAll());
        model.addAttribute("promotions", promotionService.findActiveWithImages());
        return "home";
    }
}
