package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.PhysicalCarItemDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.ContractTemplateService;
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
    private final VehicleRepository vehicleRepository;
    private final ContractTemplateService contractTemplateService;

    public VehicleController(VehicleService vehicleService,
                             VehicleTypeService vehicleTypeService,
                             VehicleRepository vehicleRepository,
                             ContractTemplateService contractTemplateService) {
        this.vehicleService = vehicleService;
        this.vehicleTypeService = vehicleTypeService;
        this.vehicleRepository = vehicleRepository;
        this.contractTemplateService = contractTemplateService;
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        VehicleDTO vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found!"));
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("contractTerms", contractTemplateService.getActiveTemplate());

        if (vehicle.getVehicleTypeId() != null) {
            List<Vehicle> cars = vehicleRepository.findByVehicleTypeIdWithDetails(vehicle.getVehicleTypeId());
            List<PhysicalCarItemDTO> physicalCars = cars.stream().map(v -> new PhysicalCarItemDTO(
                    v.getId(),
                    v.getLicensePlate() != null ? v.getLicensePlate().getPlate() : "Unassigned",
                    v.getColor(),
                    v.getYear(),
                    v.getStatus()
            )).toList();
            model.addAttribute("physicalCars", physicalCars);
        }

        return "vehicles/detail";
    }
}
