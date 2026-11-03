package com.crms.mvc.controller;

import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final PromotionService promotionService;

    public HomeController(VehicleTypeRepository vehicleTypeRepository, PromotionService promotionService) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.promotionService = promotionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1).toString());
        model.addAttribute("featuredVehicles", vehicleTypeRepository.findAll());
        model.addAttribute("promotions", promotionService.findActiveWithImages());
        return "home";
    }
}
