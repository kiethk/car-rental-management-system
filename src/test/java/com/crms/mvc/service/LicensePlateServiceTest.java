package com.crms.mvc.service;

import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.dto.LicensePlateDTO;
import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.impl.LicensePlateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LicensePlateServiceTest {
    private LicensePlateRepository licensePlateRepository;
    private VehicleRepository vehicleRepository;
    private LicensePlateService service;

    @BeforeEach
    void setUp() {
        licensePlateRepository = mock(LicensePlateRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        service = new LicensePlateServiceImpl(licensePlateRepository, vehicleRepository);
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void changePlateRetiresTheCurrentPlateAndStartsTheNewOneAtTheSameInstant() {
        Vehicle vehicle = vehicleWithPlate("available", "51A-123.45");
        LicensePlate old = vehicle.getLicensePlate();
        when(vehicleRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(vehicle));
        when(licensePlateRepository.existsByPlateIgnoreCase("51K-987.65")).thenReturn(false);

        LicensePlateDTO created = service.changePlate(1L, form("  51k-987.65 ", " Ho Chi Minh City "));

        assertEquals("51K-987.65", created.getPlate());
        assertEquals("Ho Chi Minh City", created.getProvince());
        assertTrue(created.isCurrent());
        assertNotNull(old.getValidTo(), "old plate gets an end date");
        assertEquals(old.getValidTo(), created.getValidFrom(), "no gap and no overlap between the two plates");
        assertEquals("51K-987.65", vehicle.getLicensePlate().getPlate(), "vehicle now reports the new plate");
        // The old row must reach the database before the new one is inserted (one open plate per vehicle).
        InOrder order = inOrder(licensePlateRepository);
        order.verify(licensePlateRepository).saveAndFlush(old);
        order.verify(licensePlateRepository).save(any(LicensePlate.class));
    }

    @Test
    void firstPlateOfANewVehicleNeedsNoRetirement() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setStatus("available");
        when(vehicleRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(vehicle));

        LicensePlateDTO created = service.changePlate(2L, form("30A-111.11", null));

        assertNull(created.getProvince());
        verify(licensePlateRepository, never()).saveAndFlush(any());
        assertEquals("30A-111.11", vehicle.getLicensePlate().getPlate());
    }

    @Test
    void plateCannotBeChangedWhileTheCarIsRentedOut() {
        when(vehicleRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(vehicleWithPlate("rented", "51A-123.45")));

        assertThrows(IllegalStateException.class, () -> service.changePlate(1L, form("51K-987.65", null)));
        verify(licensePlateRepository, never()).save(any());
    }

    @Test
    void aRetiredPlateCanNeverBeReused() {
        when(vehicleRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(vehicleWithPlate("available", "51A-123.45")));
        when(licensePlateRepository.existsByPlateIgnoreCase("51F-100.01")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.changePlate(1L, form("51f-100.01", null)));

        assertTrue(error.getMessage().contains("cannot be reused"));
        verify(licensePlateRepository, never()).saveAndFlush(any());
    }

    @Test
    void sameAsCurrentPlateIsRejected() {
        when(vehicleRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(vehicleWithPlate("available", "51A-123.45")));

        assertThrows(IllegalArgumentException.class, () -> service.changePlate(1L, form("51a-123.45", null)));
    }

    @Test
    void unknownVehicleIsReportedAsNotFound() {
        when(vehicleRepository.findByIdWithDetails(9L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.changePlate(9L, form("51K-987.65", null)));
    }

    @Test
    void overviewHasOneRowPerVehicleEvenWithoutAPlate() {
        Vehicle withPlate = vehicleWithPlate("available", "51A-123.45");
        withPlate.setRegistrationExpiry(LocalDate.now().plusDays(10));
        Vehicle withoutPlate = new Vehicle();
        withoutPlate.setId(3L);
        withoutPlate.setStatus("maintenance");
        withoutPlate.setVehicleType(withPlate.getVehicleType());
        withoutPlate.setRegistrationExpiry(LocalDate.now().minusDays(3));
        when(vehicleRepository.findAllWithDetails()).thenReturn(List.of(withPlate, withoutPlate));

        List<LicensePlateOverviewDTO> rows = service.findAllOverviews();

        assertEquals(2, rows.size());
        assertEquals("51A-123.45", rows.get(0).getPlate());
        assertEquals("EXPIRING_SOON", rows.get(0).getExpiryStatus());
        assertNull(rows.get(1).getPlate());
        assertEquals("EXPIRED", rows.get(1).getExpiryStatus());
    }

    private Vehicle vehicleWithPlate(String status, String plateText) {
        VehicleType type = new VehicleType();
        type.setBrand("Toyota");
        type.setName("Vios");
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setStatus(status);
        vehicle.setVehicleType(type);
        LicensePlate plate = new LicensePlate();
        plate.setId(10L);
        plate.setVehicle(vehicle);
        plate.setPlate(plateText);
        plate.setValidFrom(OffsetDateTime.now().minusYears(1));
        vehicle.setCurrentPlate(plate);
        return vehicle;
    }

    private LicensePlateChangeDTO form(String plate, String province) {
        LicensePlateChangeDTO form = new LicensePlateChangeDTO();
        form.setPlate(plate);
        form.setProvince(province);
        return form;
    }
}
