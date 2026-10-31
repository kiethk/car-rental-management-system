package com.crms.mvc.controller;

import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleInspectionService;
import com.crms.mvc.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminDashboardController {

    private final VehicleService vehicleService;
    private final VehicleInspectionService vehicleInspectionService;
    private final LicensePlateService licensePlateService;
    private final BookingService bookingService;

    public AdminDashboardController(VehicleService vehicleService,
                                    VehicleInspectionService vehicleInspectionService,
                                    LicensePlateService licensePlateService,
                                    BookingService bookingService) {
        this.vehicleService = vehicleService;
        this.vehicleInspectionService = vehicleInspectionService;
        this.licensePlateService = licensePlateService;
        this.bookingService = bookingService;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        long totalVehicles = vehicles.size();
        long availableVehicles = vehicles.stream().filter(v -> "available".equalsIgnoreCase(v.getStatus())).count();
        long rentedVehicles = vehicles.stream().filter(v -> "rented".equalsIgnoreCase(v.getStatus())).count();
        long maintenanceVehicles = vehicles.stream().filter(v -> "maintenance".equalsIgnoreCase(v.getStatus())).count();

        List<VehicleInspection> inspections = vehicleInspectionService.findAll();
        long scheduledInspections = inspections.stream().filter(i -> "scheduled".equalsIgnoreCase(i.getStatus())).count();
        long passedInspections = inspections.stream().filter(i -> "passed".equalsIgnoreCase(i.getStatus())).count();
        long failedInspections = inspections.stream().filter(i -> "failed".equalsIgnoreCase(i.getStatus())).count();

        long totalBookings = bookingService.countTotalBookings();

        List<LicensePlateOverviewDTO> plates = licensePlateService.findAllOverviews();
        long expiringSoonCount = plates.stream().filter(p -> "EXPIRING_SOON".equals(p.getExpiryStatus())).count();
        long expiredCount = plates.stream().filter(p -> "EXPIRED".equals(p.getExpiryStatus())).count();

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("rentedVehicles", rentedVehicles);
        model.addAttribute("maintenanceVehicles", maintenanceVehicles);

        model.addAttribute("scheduledInspections", scheduledInspections);
        model.addAttribute("passedInspections", passedInspections);
        model.addAttribute("failedInspections", failedInspections);

        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("expiringSoonCount", expiringSoonCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("recentInspections", inspections.stream().limit(6).toList());
        model.addAttribute("activeNav", "dashboard");

        return "admin/dashboard";
    }

    @GetMapping("/admin/ui-kit")
    public String uiKit() {
        return "admin/starter-kit";
    }
}
