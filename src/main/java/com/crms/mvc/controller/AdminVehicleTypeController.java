package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.VehicleTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/vehicle-types")
public class AdminVehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    public AdminVehicleTypeController(VehicleTypeService vehicleTypeService) {
        this.vehicleTypeService = vehicleTypeService;
    }

    @GetMapping
    public String list(Model model) {
        List<VehicleType> types = vehicleTypeService.findAll();
        Map<Long, Long> counts = new HashMap<>();
        for (VehicleType vt : types) {
            counts.put(vt.getId(), vehicleTypeService.countVehiclesOfType(vt.getId()));
        }

        model.addAttribute("vehicleTypes", types);
        model.addAttribute("vehicleCounts", counts);
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicle-types/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("typeForm")) {
            model.addAttribute("typeForm", new VehicleTypeFormDTO());
        }
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicle-types/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("typeForm") VehicleTypeFormDTO form,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeNav", "vehicles");
            return "admin/vehicle-types/form";
        }

        try {
            VehicleType created = vehicleTypeService.create(form);
            ra.addFlashAttribute("success", "Vehicle model created successfully: " + created.getBrand() + " " + created.getName());
            return "redirect:/admin/vehicle-types";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("typeForm", form);
            return "redirect:/admin/vehicle-types/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        VehicleType vt = vehicleTypeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle model not found for ID: " + id));

        if (!model.containsAttribute("typeForm")) {
            model.addAttribute("typeForm", VehicleTypeFormDTO.fromEntity(vt));
        }
        model.addAttribute("vehicleType", vt);
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicle-types/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("typeForm") VehicleTypeFormDTO form,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            VehicleType vt = vehicleTypeService.findById(id).orElse(null);
            model.addAttribute("vehicleType", vt);
            model.addAttribute("activeNav", "vehicles");
            return "admin/vehicle-types/form";
        }

        try {
            VehicleType updated = vehicleTypeService.update(id, form);
            ra.addFlashAttribute("success", "Vehicle model updated successfully: " + updated.getBrand() + " " + updated.getName());
            return "redirect:/admin/vehicle-types";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vehicle-types/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            vehicleTypeService.delete(id);
            ra.addFlashAttribute("success", "Vehicle model deleted ID: " + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vehicle-types";
    }
}
