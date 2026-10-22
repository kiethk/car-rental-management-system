package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.service.VehicleInspectionService;
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
@RequestMapping("/admin/inspections")
public class AdminInspectionController {

    private final VehicleInspectionService vehicleInspectionService;
    private final VehicleService vehicleService;

    public AdminInspectionController(VehicleInspectionService vehicleInspectionService,
                                     VehicleService vehicleService) {
        this.vehicleInspectionService = vehicleInspectionService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String list(Model model) {
        List<VehicleInspection> inspections = vehicleInspectionService.findAll();
        long scheduledCount = inspections.stream().filter(i -> "scheduled".equalsIgnoreCase(i.getStatus())).count();
        long passedCount = inspections.stream().filter(i -> "passed".equalsIgnoreCase(i.getStatus())).count();
        long failedCount = inspections.stream().filter(i -> "failed".equalsIgnoreCase(i.getStatus())).count();

        model.addAttribute("inspections", inspections);
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("activeNav", "inspections");
        return "admin/inspections/list";
    }

    @GetMapping("/new")
    public String scheduleForm(@RequestParam(value = "vehicleId", required = false) Long preselectedVehicleId,
                               Model model) {
        VehicleInspectionFormDTO form = new VehicleInspectionFormDTO();
        if (preselectedVehicleId != null) {
            form.setVehicleId(preselectedVehicleId);
        }

        List<Vehicle> vehicles = vehicleService.findAll();
        model.addAttribute("inspectionForm", form);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("activeNav", "inspections");
        return "admin/inspections/schedule";
    }

    @PostMapping
    public String schedule(@Valid @ModelAttribute("inspectionForm") VehicleInspectionFormDTO form,
                           BindingResult result,
                           RedirectAttributes ra,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleService.findAll());
            model.addAttribute("activeNav", "inspections");
            return "admin/inspections/schedule";
        }

        try {
            VehicleInspection scheduled = vehicleInspectionService.schedule(form);
            ra.addFlashAttribute("success", "Đã lên lịch kiểm định cho xe #" + scheduled.getVehicle().getId() + " (" + scheduled.getVehicle().getLicensePlate().getPlate() + ")");
            return "redirect:/admin/inspections";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("inspectionForm", form);
            return "redirect:/admin/inspections/new";
        }
    }

    @PostMapping("/{id}/start")
    public String startInspection(@PathVariable Long id, RedirectAttributes ra) {
        try {
            vehicleInspectionService.startInspection(id);
            ra.addFlashAttribute("success", "Xe đã được đưa vào xưởng kiểm định. Trạng thái xe đổi sang ĐANG BẢO DƯỠNG (maintenance).");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/inspections";
    }

    @GetMapping("/{id}/result")
    public String resultForm(@PathVariable Long id, Model model) {
        VehicleInspection inspection = vehicleInspectionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt kiểm định ID: " + id));

        VehicleInspectionFormDTO form = VehicleInspectionFormDTO.fromEntity(inspection);
        if (form.getNewRegistrationExpiry() == null) {
            form.setNewRegistrationExpiry(LocalDate.now().plusYears(1));
        }

        model.addAttribute("inspection", inspection);
        model.addAttribute("resultForm", form);
        model.addAttribute("activeNav", "inspections");
        return "admin/inspections/result";
    }

    @PostMapping("/{id}/result")
    public String saveResult(@PathVariable Long id,
                             @RequestParam("status") String status,
                             @RequestParam(value = "notes", required = false) String notes,
                             @RequestParam(value = "newRegistrationExpiry", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newRegistrationExpiry,
                             RedirectAttributes ra) {
        try {
            vehicleInspectionService.recordResult(id, status, notes, newRegistrationExpiry);
            if ("passed".equalsIgnoreCase(status)) {
                ra.addFlashAttribute("success", "Ghi nhận kiểm định ĐẠT thành công! Xe đã trở về trạng thái SẴN SÀNG (available) và hạn đăng kiểm đã được gia hạn.");
            } else {
                ra.addFlashAttribute("warning", "Ghi nhận kiểm định KHÔNG ĐẠT. Xe tiếp tục ở trạng thái ĐANG BẢO DƯỠNG (maintenance) để sửa chữa.");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/inspections";
    }
}
