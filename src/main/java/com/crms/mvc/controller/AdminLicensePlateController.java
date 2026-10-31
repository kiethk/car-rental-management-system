package com.crms.mvc.controller;

import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

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

    /** Registration monitor: one row per vehicle with its current plate. */
    @GetMapping
    public String list(Model model) {
        List<LicensePlateOverviewDTO> rows = licensePlateService.findAllOverviews();

        long expiredCount = rows.stream().filter(p -> "EXPIRED".equals(p.getExpiryStatus())).count();
        long expiringSoonCount = rows.stream().filter(p -> "EXPIRING_SOON".equals(p.getExpiryStatus())).count();
        long validCount = rows.stream().filter(p -> "VALID".equals(p.getExpiryStatus())).count();

        model.addAttribute("plates", rows);
        model.addAttribute("totalPlates", rows.size());
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("expiringSoonCount", expiringSoonCount);
        model.addAttribute("validCount", validCount);
        model.addAttribute("activeNav", "vehicles");
        return "admin/license-plates/list";
    }

    /** Plate history of one vehicle plus the form to put a new plate on it. */
    @GetMapping("/vehicle/{vehicleId}")
    public String history(@PathVariable Long vehicleId, Model model) {
        Vehicle vehicle = vehicleService.findEntityById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found with ID: " + vehicleId));
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
                    .orElseThrow(() -> new IllegalArgumentException("Car not found with ID: " + vehicleId));
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

    @PostMapping("/vehicle/{vehicleId}/registration")
    public String updateRegistration(@PathVariable Long vehicleId,
                                     @RequestParam(value = "registrationExpiry", required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationExpiry,
                                     RedirectAttributes ra) {
        try {
            vehicleService.updateRegistrationExpiry(vehicleId, registrationExpiry);
            ra.addFlashAttribute("success", "Registration expiry updated for car #" + vehicleId + ".");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/license-plates";
    }

    private void addHistoryModel(Model model, Vehicle vehicle) {
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("history", licensePlateService.findHistory(vehicle.getId()));
        model.addAttribute("activeNav", "vehicles");
    }
}
