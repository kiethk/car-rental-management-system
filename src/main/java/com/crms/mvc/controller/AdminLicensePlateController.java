package com.crms.mvc.controller;

import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.service.LicensePlateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/license-plates")
public class AdminLicensePlateController {

    private final LicensePlateService licensePlateService;

    public AdminLicensePlateController(LicensePlateService licensePlateService) {
        this.licensePlateService = licensePlateService;
    }

    @GetMapping
    public String list(Model model) {
        List<LicensePlateOverviewDTO> plates = licensePlateService.findAllOverviews();

        long expiredCount = plates.stream().filter(p -> "EXPIRED".equals(p.getExpiryStatus())).count();
        long expiringSoonCount = plates.stream().filter(p -> "EXPIRING_SOON".equals(p.getExpiryStatus())).count();
        long validCount = plates.stream().filter(p -> "VALID".equals(p.getExpiryStatus())).count();

        model.addAttribute("plates", plates);
        model.addAttribute("totalPlates", plates.size());
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("expiringSoonCount", expiringSoonCount);
        model.addAttribute("validCount", validCount);
        model.addAttribute("activeNav", "vehicles");
        return "admin/license-plates/list";
    }

    @PostMapping("/{id}/quick-update")
    public String quickUpdate(@PathVariable Long id,
                              @RequestParam(value = "province", required = false) String province,
                              @RequestParam(value = "registrationExpiry", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationExpiry,
                              RedirectAttributes ra) {
        try {
            licensePlateService.update(id, province, registrationExpiry);
            ra.addFlashAttribute("success", "License plate updated successfully!");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/license-plates";
    }
}
