package com.crms.mvc.controller;
import com.crms.mvc.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class VehicleController {
    private final VehicleService vehicleService;
    public VehicleController(VehicleService vehicleService) { this.vehicleService = vehicleService; }
    @GetMapping({"/", "/vehicles"})
    public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAttribute("vehicles", vehicleService.search(keyword));
        model.addAttribute("keyword", keyword);
        return "vehicles/list";
    }
    @GetMapping("/vehicles/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "vehicles/detail";
    }
}
