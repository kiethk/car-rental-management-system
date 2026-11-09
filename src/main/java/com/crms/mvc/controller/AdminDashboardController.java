package com.crms.mvc.controller;

import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.service.BookingQueryService;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleInspectionService;
import com.crms.mvc.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Comparator;

@Controller
public class AdminDashboardController {

    private final VehicleService vehicleService;
    private final VehicleInspectionService vehicleInspectionService;
    private final LicensePlateService licensePlateService;
    private final BookingQueryService bookingQueryService;

    public AdminDashboardController(VehicleService vehicleService,
                                    VehicleInspectionService vehicleInspectionService,
                                    LicensePlateService licensePlateService,
                                    BookingQueryService bookingQueryService) {
        this.vehicleService = vehicleService;
        this.vehicleInspectionService = vehicleInspectionService;
        this.licensePlateService = licensePlateService;
        this.bookingQueryService = bookingQueryService;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        long retiredVehicles = vehicles.stream().filter(Vehicle::isRetired).count();
        long totalVehicles = vehicles.size() - retiredVehicles;
        long availableVehicles = vehicles.stream().filter(v -> Vehicle.STATUS_AVAILABLE.equalsIgnoreCase(v.getStatus())).count();
        long rentedVehicles = vehicles.stream().filter(v -> Vehicle.STATUS_RENTED.equalsIgnoreCase(v.getStatus())).count();
        long maintenanceVehicles = vehicles.stream().filter(v -> Vehicle.STATUS_MAINTENANCE.equalsIgnoreCase(v.getStatus())).count();

        List<VehicleInspection> inspections = vehicleInspectionService.findAll();
        long scheduledInspections = inspections.stream().filter(i -> "scheduled".equalsIgnoreCase(i.getStatus())).count();
        long passedInspections = inspections.stream().filter(i -> "passed".equalsIgnoreCase(i.getStatus())).count();
        long failedInspections = inspections.stream().filter(i -> "failed".equalsIgnoreCase(i.getStatus())).count();

        long totalBookings = bookingQueryService.countTotalBookings();

        List<LicensePlateOverviewDTO> plates = licensePlateService.findAllOverviews();
        // Retired cars are out of service for good, so their registration no longer matters. Soonest expiry first.
        List<LicensePlateOverviewDTO> expiringSoon = registrationAlerts(plates, "EXPIRING_SOON");
        List<LicensePlateOverviewDTO> expired = registrationAlerts(plates, "EXPIRED");

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("retiredVehicles", retiredVehicles);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("rentedVehicles", rentedVehicles);
        model.addAttribute("maintenanceVehicles", maintenanceVehicles);

        model.addAttribute("scheduledInspections", scheduledInspections);
        model.addAttribute("passedInspections", passedInspections);
        model.addAttribute("failedInspections", failedInspections);

        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("expiringSoonCount", expiringSoon.size());
        model.addAttribute("expiredCount", expired.size());
        model.addAttribute("expiringSoonList", expiringSoon);
        model.addAttribute("expiredList", expired);
        model.addAttribute("recentInspections", inspections.stream().limit(6).toList());
        model.addAttribute("activeNav", "dashboard");

        return "screens/admin/dashboard/index";
    }

    @GetMapping("/admin/ui-kit")
    public String uiKit() {
        return "screens/admin/starter-kit/index";
    }

    private static List<LicensePlateOverviewDTO> registrationAlerts(List<LicensePlateOverviewDTO> plates, String status) {
        return plates.stream()
                .filter(p -> status.equals(p.getExpiryStatus()))
                .filter(p -> !Vehicle.STATUS_RETIRED.equalsIgnoreCase(p.getVehicleStatus()))
                .sorted(Comparator.comparing(LicensePlateOverviewDTO::getRegistrationExpiry))
                .toList();
    }
}
