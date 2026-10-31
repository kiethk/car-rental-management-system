package com.crms.mvc.controller;

import com.crms.mvc.dto.PageNavDTO;
import com.crms.mvc.dto.PageQueryDTO;
import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.VehicleTypeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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

    private static final String LIST_PATH = "/admin/vehicle-types";

    private final VehicleTypeService vehicleTypeService;

    public AdminVehicleTypeController(VehicleTypeService vehicleTypeService) {
        this.vehicleTypeService = vehicleTypeService;
    }

    @GetMapping
    public String list(@ModelAttribute("query") PageQueryDTO query,
                       BindingResult ignoredBindingErrors, // a mistyped ?page=abc keeps the default instead of failing the page
                       Model model) {
        Page<VehicleType> page = vehicleTypeService.searchForAdmin(query);
        // The service steps back to the last page when the requested one no longer exists
        query.setPage(page.getNumber() + 1);

        Map<Long, Long> counts = new HashMap<>();
        for (VehicleType vt : page.getContent()) {
            counts.put(vt.getId(), vehicleTypeService.countVehiclesOfType(vt.getId()));
        }

        model.addAttribute("vehicleTypes", page.getContent());
        model.addAttribute("vehicleCounts", counts);
        model.addAttribute("nav", PageNavDTO.of(page, number -> query.urlForPage(LIST_PATH, number)));
        model.addAttribute("listUrl", query.urlForPage(LIST_PATH, query.getPage()));
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
    public String delete(@PathVariable Long id,
                         @RequestParam(value = "returnTo", required = false) String returnTo,
                         RedirectAttributes ra) {
        try {
            vehicleTypeService.delete(id);
            ra.addFlashAttribute("success", "Vehicle model deleted ID: " + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        // Back to the list as the admin left it; only this controller's own list URLs are accepted
        boolean ownList = returnTo != null && returnTo.startsWith(LIST_PATH)
                && !returnTo.contains("//") && !returnTo.contains("\\");
        return "redirect:" + (ownList ? returnTo : LIST_PATH);
    }
}
