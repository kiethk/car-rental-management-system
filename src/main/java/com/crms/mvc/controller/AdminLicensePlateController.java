package com.crms.mvc.controller;

import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/license-plates")
public class AdminLicensePlateController {

    private final LicensePlateService licensePlateService;
    private final VehicleService vehicleService;

    public AdminLicensePlateController(LicensePlateService licensePlateService,
                                       VehicleService vehicleService) {
        this.licensePlateService = licensePlateService;
        this.vehicleService = vehicleService;
    }

    /** Plate history of one vehicle plus the form to put a new plate on it. */
    @GetMapping("/vehicle/{vehicleId}")
    public String history(@PathVariable Long vehicleId, Model model) {
        Vehicle vehicle = vehicleService.findEntityById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe có mã: " + vehicleId));
        if (!model.containsAttribute("plateForm")) {
            model.addAttribute("plateForm", new LicensePlateChangeDTO());
        }
        addHistoryModel(model, vehicle);
        return "admin/license-plates/history";
    }

    @PostMapping("/vehicle/{vehicleId}/change")
    public String changePlate(@PathVariable Long vehicleId,
                              @Valid @ModelAttribute("plateForm") LicensePlateChangeDTO form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            Vehicle vehicle = vehicleService.findEntityById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe có mã: " + vehicleId));
            addHistoryModel(model, vehicle);
            return "admin/license-plates/history";
        }
        try {
            licensePlateService.changePlate(vehicleId, form);
            ra.addFlashAttribute("success", "New plate " + form.getPlate().trim().toUpperCase() + " is now on car #" + vehicleId + ". The previous plate was kept in the history.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("plateForm", form);
        }
        return "redirect:/admin/license-plates/vehicle/" + vehicleId;
    }

    private void addHistoryModel(Model model, Vehicle vehicle) {
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("history", licensePlateService.findHistory(vehicle.getId()));
        model.addAttribute("activeNav", "vehicles");
    }
}
