package com.crms.mvc.controller;

import com.crms.mvc.dto.AdminVehicleQueryDTO;
import com.crms.mvc.dto.PageNavDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/vehicles")
public class AdminVehicleController {

    private static final String LIST_PATH = "/admin/vehicles";

    private final VehicleService vehicleService;
    private final VehicleTypeService vehicleTypeService;

    public AdminVehicleController(VehicleService vehicleService,
                                  VehicleTypeService vehicleTypeService) {
        this.vehicleService = vehicleService;
        this.vehicleTypeService = vehicleTypeService;
    }

    @GetMapping
    public String list(@ModelAttribute("query") AdminVehicleQueryDTO query,
                       BindingResult ignoredBindingErrors, // a mistyped ?page=abc keeps the default instead of failing the page
                       Model model) {
        Page<VehicleDTO> page = vehicleService.searchForAdmin(query);
        // The service steps back to the last page when the requested one no longer exists
        query.setPage(page.getNumber() + 1);

        Map<String, Long> statusCounts = vehicleService.countByStatus();
        model.addAttribute("vehicles", page.getContent());
        model.addAttribute("nav", PageNavDTO.of(page, number -> query.urlForPage(LIST_PATH, number)));
        model.addAttribute("listUrl", query.urlForPage(LIST_PATH, query.getPage()));
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("totalCars", statusCounts.values().stream().mapToLong(Long::longValue).sum());
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
            ra.addFlashAttribute("success", "Car created successfully! ID: #" + created.getId() + " - Plate: " + created.getLicensePlate().getPlate());
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
                .orElseThrow(() -> new IllegalArgumentException("Car not found with ID: " + id));

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
            ra.addFlashAttribute("success", "Car #" + updated.getId() + " updated successfully!");
            return "redirect:/admin/vehicles";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vehicles/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    public String toggleStatus(@PathVariable Long id,
                               @RequestParam("status") String status,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               RedirectAttributes ra) {
        try {
            vehicleService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Car #" + id + " status updated to " + status.toUpperCase());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToList(returnTo);
    }

    @PostMapping("/{id}/odometer")
    public String updateOdometer(@PathVariable Long id,
                                 @RequestParam("odometer") Integer odometer,
                                 @RequestParam(value = "returnTo", required = false) String returnTo,
                                 RedirectAttributes ra) {
        try {
            vehicleService.updateOdometer(id, odometer);
            ra.addFlashAttribute("success", "Odometer reading updated for car #" + id + ": " + odometer + " km");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToList(returnTo);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(value = "returnTo", required = false) String returnTo,
                         RedirectAttributes ra) {
        try {
            vehicleService.delete(id);
            ra.addFlashAttribute("success", "Car #" + id + " deleted successfully!");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToList(returnTo);
    }

    /**
     * Back to the list exactly as the admin left it (same search, sort and page).
     * Only this controller's own list URLs are accepted, so the parameter cannot be used as an open redirect.
     */
    private String redirectToList(String returnTo) {
        boolean ownList = returnTo != null && returnTo.startsWith(LIST_PATH)
                && !returnTo.contains("//") && !returnTo.contains("\\");
        return "redirect:" + (ownList ? returnTo : LIST_PATH);
    }
}
