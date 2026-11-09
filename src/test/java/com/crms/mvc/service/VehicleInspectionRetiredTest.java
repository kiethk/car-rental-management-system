package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.impl.VehicleInspectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A retired vehicle is out of service for good, so inspections must never bring it back. */
class VehicleInspectionRetiredTest {
    private VehicleInspectionRepository inspectionRepository;
    private VehicleRepository vehicleRepository;
    private VehicleInspectionService service;
    private Vehicle retired;

    @BeforeEach
    void setUp() {
        inspectionRepository = mock(VehicleInspectionRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        service = new VehicleInspectionServiceImpl(inspectionRepository, vehicleRepository,
                mock(com.crms.mvc.repository.BookingRepository.class));

        retired = new Vehicle();
        retired.setId(7L);
        retired.setStatus(Vehicle.STATUS_RETIRED);
    }

    @Test
    void cannotScheduleAnInspectionForARetiredVehicle() {
        when(vehicleRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(retired));
        VehicleInspectionFormDTO form = new VehicleInspectionFormDTO();
        form.setVehicleId(7L);

        assertThrows(IllegalStateException.class, () -> service.schedule(form));

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void startingAnInspectionKeepsARetiredVehicleRetired() {
        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspectionOf(retired)));

        assertThrows(IllegalStateException.class, () -> service.startInspection(1L));

        assertEquals(Vehicle.STATUS_RETIRED, retired.getStatus());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void aPassedInspectionDoesNotBringARetiredVehicleBack() {
        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspectionOf(retired)));

        assertThrows(IllegalStateException.class, () -> service.recordResult(1L, "passed", null, null));

        assertEquals(Vehicle.STATUS_RETIRED, retired.getStatus());
        verify(vehicleRepository, never()).save(any());
    }

    private VehicleInspection inspectionOf(Vehicle vehicle) {
        VehicleInspection inspection = new VehicleInspection();
        inspection.setVehicle(vehicle);
        inspection.setStatus("scheduled");
        return inspection;
    }
}
