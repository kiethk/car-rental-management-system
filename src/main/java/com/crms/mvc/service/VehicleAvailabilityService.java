package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleScheduleDTO;
import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Vehicle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Which cars can be rented when: free of other rentals (with the buffer between two rentals), in service, and not
 * kept off the road by an inspection. The one place that answers "is this car free?" for search, checkout, booking
 * and the per-car schedule.
 */
public interface VehicleAvailabilityService {
    /** Whole hours kept free between the end of one rental and the start of the next on the same car. */
    int BUFFER_BETWEEN_RENTALS_HOURS = 1;

    /** The cars of one vehicle type that are free for the whole window. */
    List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);

    /** Hour-by-hour state of one car for {@code days} days from {@code fromDate}. */
    VehicleScheduleDTO getVehicleSchedule(Long vehicleId, LocalDate fromDate, int days);

    /** Every vehicle type (optionally of one category) with how many of its cars are free for the window. */
    List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category);
}
