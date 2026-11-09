package com.crms.mvc.controller;

import com.crms.mvc.dto.PhysicalCarItemDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleScheduleDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.VehicleAvailabilityService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.transaction.annotation.Transactional;
import com.crms.mvc.util.DateTimeUtils;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleTypeService vehicleTypeService;
    private final ContractTemplateService contractTemplateService;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final ReviewService reviewService;

    /** How far ahead a customer can see one car's schedule. */
    static final int SCHEDULE_DAYS = 30;

    public VehicleController(VehicleService vehicleService,
                             VehicleTypeService vehicleTypeService,
                             ContractTemplateService contractTemplateService,
                             VehicleAvailabilityService vehicleAvailabilityService,
                             ReviewService reviewService) {
        this.vehicleService = vehicleService;
        this.vehicleTypeService = vehicleTypeService;
        this.contractTemplateService = contractTemplateService;
        this.vehicleAvailabilityService = vehicleAvailabilityService;
        this.reviewService = reviewService;
    }

    @GetMapping("/vehicles")
    public String list(@ModelAttribute("filter") VehicleSearchFilterDTO filter, Model model) {
        List<VehicleDTO> vehicles = vehicleService.searchWithFilter(filter);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("brands", vehicleTypeService.getDistinctBrands());
        model.addAttribute("categories", vehicleTypeService.getDistinctCategories());
        model.addAttribute("totalCount", vehicles.size());
        model.addAttribute("ratings", reviewService.ratingsByVehicleType());
        return "screens/customer/vehicles/list/index";
    }

    @GetMapping("/vehicles/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        VehicleDTO vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe!"));
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("contractTerms", contractTemplateService.getActiveTemplate());

        if (vehicle.getVehicleTypeId() != null) {
            VehicleReviewSummaryDTO reviewSummary =
                    reviewService.findPublicReviewsForVehicleType(vehicle.getVehicleTypeId());
            model.addAttribute("reviewSummary", reviewSummary);
            List<Vehicle> cars = vehicleService.findByVehicleTypeIdWithDetails(vehicle.getVehicleTypeId());
            List<PhysicalCarItemDTO> physicalCars = cars.stream().map(v -> new PhysicalCarItemDTO(
                    v.getId(),
                    v.getLicensePlate() != null ? v.getLicensePlate().getPlate() : "Unassigned",
                    v.getColor(),
                    v.getYear(),
                    v.getStatus()
            )).toList();
            model.addAttribute("physicalCars", physicalCars);
        }

        return "screens/customer/vehicles/detail/index";
    }

    /** One car's schedule for the next 30 days, so a customer who wants that exact car can find a free window. */
    @GetMapping("/vehicles/cars/{vehicleId}/schedule")
    public String schedule(@PathVariable Long vehicleId, Model model) {
        VehicleScheduleDTO schedule;
        try {
            schedule = vehicleAvailabilityService.getVehicleSchedule(vehicleId, LocalDate.now(DateTimeUtils.VIETNAM_ZONE), SCHEDULE_DAYS);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe!");
        }
        if (Vehicle.STATUS_RETIRED.equalsIgnoreCase(schedule.getVehicleStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Xe đã ngừng hoạt động");
        }
        model.addAttribute("schedule", schedule);
        return "screens/customer/vehicles/schedule/index";
    }
}
