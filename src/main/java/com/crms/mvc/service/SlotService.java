package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import java.time.OffsetDateTime;
import java.util.List;

public interface SlotService {
    List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);
    List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category);
    Slot createSlotForBooking(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime);
}
