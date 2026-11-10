package com.crms.mvc.service;

import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A retired vehicle is out of service for good: the status can be reached, but never left. */
class VehicleRetireTest {
    private VehicleRepository vehicleRepository;
    private VehicleTypeRepository vehicleTypeRepository;
    private VehicleService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        vehicleTypeRepository = mock(VehicleTypeRepository.class);
        service = new VehicleServiceImpl(vehicleRepository, vehicleTypeRepository,
                mock(LicensePlateService.class), mock(BookingRepository.class),
                mock(VehicleInspectionRepository.class), mock(IssueRepository.class));
    }

    @Test
    void anAvailableOrMaintenanceVehicleCanBeRetired() {
        Vehicle vehicle = vehicleWithStatus(VehicleStatus.MAINTENANCE);

        service.updateStatus(7L, "retired");

        assertEquals(VehicleStatus.RETIRED, vehicle.getStatus());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void aVehicleOnTripCannotBeRetired() {
        Vehicle vehicle = vehicleWithStatus(VehicleStatus.RENTED);

        assertThrows(IllegalStateException.class, () -> service.updateStatus(7L, "retired"));

        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void aRetiredVehicleCannotComeBack() {
        Vehicle vehicle = vehicleWithStatus(VehicleStatus.RETIRED);

        assertThrows(IllegalStateException.class, () -> service.updateStatus(7L, "available"));
        assertThrows(IllegalStateException.class, () -> service.updateStatus(7L, "maintenance"));

        assertEquals(VehicleStatus.RETIRED, vehicle.getStatus());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void editingARetiredVehicleThroughTheFormKeepsItRetired() {
        Vehicle vehicle = vehicleWithStatus(VehicleStatus.RETIRED);
        VehicleType type = new VehicleType();
        type.setId(3L);
        when(vehicleTypeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleFormDTO form = new VehicleFormDTO();
        form.setVehicleTypeId(3L);
        form.setColor("Red");
        form.setYear(2024);
        form.setOdometer(1200);
        form.setStatus("available");
        form.setRegistrationExpiry(LocalDate.now().plusYears(1));

        service.update(7L, form);

        assertEquals(VehicleStatus.RETIRED, vehicle.getStatus());
        assertEquals(1200, vehicle.getOdometer());
    }

    @Test
    void editingAWorkingVehicleThroughTheFormCanRetireIt() {
        Vehicle vehicle = vehicleWithStatus(VehicleStatus.AVAILABLE);
        VehicleType type = new VehicleType();
        type.setId(3L);
        when(vehicleTypeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleFormDTO form = new VehicleFormDTO();
        form.setVehicleTypeId(3L);
        form.setColor("Red");
        form.setYear(2024);
        form.setOdometer(1200);
        form.setStatus("retired");
        form.setRegistrationExpiry(LocalDate.now().plusYears(1));

        service.update(7L, form);

        assertEquals(VehicleStatus.RETIRED, vehicle.getStatus());
    }

    @Test
    void unknownStatusesAreStillRejected() {
        vehicleWithStatus(VehicleStatus.AVAILABLE);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(7L, "scrapped"));
    }

    private Vehicle vehicleWithStatus(VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setStatus(status);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }
}
