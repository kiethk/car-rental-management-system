package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleTypeService vehicleTypeService;

    public VehicleController(VehicleService vehicleService, VehicleTypeService vehicleTypeService) {
        this.vehicleService = vehicleService;
        this.vehicleTypeService = vehicleTypeService;
    }

    @GetMapping("/vehicles")
    public String list(@ModelAttribute("filter") VehicleSearchFilterDTO filter, Model model) {
        List<VehicleDTO> vehicles = vehicleService.searchWithFilter(filter);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("brands", vehicleTypeService.getDistinctBrands());
        model.addAttribute("categories", vehicleTypeService.getDistinctCategories());
        model.addAttribute("totalCount", vehicles.size());
        return "vehicles/list";
    }

    @GetMapping("/vehicles/{id}")
    public String detail(@PathVariable Long id, Model model) {
        VehicleDTO vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe!"));
        model.addAttribute("vehicle", vehicle);
        return "vehicles/detail";
    }
}
