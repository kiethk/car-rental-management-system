package com.crms.mvc.controller;

import com.crms.mvc.repository.VehicleTypeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final VehicleTypeRepository vehicleTypeRepository;

    public HomeController(VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1).toString());
        model.addAttribute("featuredVehicles", vehicleTypeRepository.findAll());
        return "home";
    }
}

