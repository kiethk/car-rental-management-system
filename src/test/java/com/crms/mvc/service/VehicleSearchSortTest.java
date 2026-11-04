package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Customer-facing sorting: by price and alphabetically by what the card shows (brand + model). */
class VehicleSearchSortTest {
    private VehicleService service;

    @BeforeEach
    void setUp() {
        VehicleTypeRepository vehicleTypeRepository = mock(VehicleTypeRepository.class);
        when(vehicleTypeRepository.findAll()).thenReturn(List.of(
                type("Vios", "Toyota", "150000"),
                type("Accent", "Hyundai", "160000"),
                type("Morning", "Kia", "120000"),
                type("Toyota Camry", "Toyota", "350000")));
        service = new VehicleServiceImpl(mock(VehicleRepository.class), vehicleTypeRepository,
                mock(LicensePlateService.class), mock(BookingRepository.class),
                mock(VehicleInspectionRepository.class), mock(IssueRepository.class));
    }

    @Test
    void priceAscendingIsTheDefault() {
        assertEquals(List.of("Kia Morning", "Toyota Vios", "Hyundai Accent", "Toyota Camry"), names(filter(null)));
        assertEquals(List.of("Kia Morning", "Toyota Vios", "Hyundai Accent", "Toyota Camry"), names(filter("price_asc")));
    }

    @Test
    void priceDescending() {
        assertEquals(List.of("Toyota Camry", "Hyundai Accent", "Toyota Vios", "Kia Morning"), names(filter("price_desc")));
    }

    @Test
    void alphabeticalUsesTheDisplayNameWithBrand() {
        assertEquals(List.of("Hyundai Accent", "Kia Morning", "Toyota Camry", "Toyota Vios"), names(filter("name_asc")));
        assertEquals(List.of("Toyota Vios", "Toyota Camry", "Kia Morning", "Hyundai Accent"), names(filter("name_desc")));
    }

    @Test
    void unsupportedSortFallsBackToPrice() {
        assertEquals(List.of("Kia Morning", "Toyota Vios", "Hyundai Accent", "Toyota Camry"), names(filter("year_desc")));
    }

    private VehicleSearchFilterDTO filter(String sortBy) {
        VehicleSearchFilterDTO filter = new VehicleSearchFilterDTO();
        if (sortBy != null) {
            filter.setSortBy(sortBy);
        }
        return filter;
    }

    private List<String> names(VehicleSearchFilterDTO filter) {
        return service.searchWithFilter(filter).stream().map(VehicleDTO::getDisplayName).toList();
    }

    private VehicleType type(String name, String brand, String price) {
        VehicleType type = new VehicleType();
        type.setName(name);
        type.setBrand(brand);
        type.setPricePerHour(new BigDecimal(price));
        return type;
    }
}
