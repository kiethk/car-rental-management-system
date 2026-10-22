package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/vehicles")
public class AdminVehicleController {

    private final VehicleService vehicleService;
    private final VehicleTypeService vehicleTypeService;

    public AdminVehicleController(VehicleService vehicleService,
                                  VehicleTypeService vehicleTypeService) {
        this.vehicleService = vehicleService;
        this.vehicleTypeService = vehicleTypeService;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status, Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            vehicles = vehicles.stream()
                    .filter(v -> status.equalsIgnoreCase(v.getStatus()))
                    .toList();
        }

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("selectedStatus", status != null ? status : "all");
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicles/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("vehicleForm")) {
            model.addAttribute("vehicleForm", new VehicleFormDTO());
        }
        model.addAttribute("vehicleTypes", vehicleTypeService.findAll());
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicles/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("vehicleForm") VehicleFormDTO form,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("vehicleTypes", vehicleTypeService.findAll());
            model.addAttribute("activeNav", "vehicles");
            return "admin/vehicles/form";
        }

        try {
            Vehicle created = vehicleService.create(form);
            ra.addFlashAttribute("success", "Thêm xe thành công! Mã xe: #" + created.getId() + " - Biển số: " + created.getLicensePlate().getPlate());
            return "redirect:/admin/vehicles";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("vehicleForm", form);
            return "redirect:/admin/vehicles/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.findEntityById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe ID: " + id));

        if (!model.containsAttribute("vehicleForm")) {
            model.addAttribute("vehicleForm", VehicleFormDTO.fromEntity(vehicle));
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("vehicleTypes", vehicleTypeService.findAll());
        model.addAttribute("activeNav", "vehicles");
        return "admin/vehicles/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("vehicleForm") VehicleFormDTO form,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Vehicle vehicle = vehicleService.findEntityById(id).orElse(null);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleTypes", vehicleTypeService.findAll());
            model.addAttribute("activeNav", "vehicles");
            return "admin/vehicles/form";
        }

        try {
            Vehicle updated = vehicleService.update(id, form);
            ra.addFlashAttribute("success", "Cập nhật xe #" + updated.getId() + " thành công!");
            return "redirect:/admin/vehicles";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vehicles/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    public String toggleStatus(@PathVariable Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes ra) {
        try {
            vehicleService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Đã đổi trạng thái xe #" + id + " thành " + status.toUpperCase());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/{id}/odometer")
    public String updateOdometer(@PathVariable Long id,
                                 @RequestParam("odometer") Integer odometer,
                                 RedirectAttributes ra) {
        try {
            vehicleService.updateOdometer(id, odometer);
            ra.addFlashAttribute("success", "Cập nhật số km (odometer) xe #" + id + " thành công: " + odometer + " km");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            vehicleService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa xe #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vehicles";
    }
}
