package com.crms.mvc.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The filters of the search page narrow the results and never reorder them. */
class RentalSearchCriteriaTest {
    private final VehicleTypeSearchResultDTO vios = car("Vios", "Toyota", 5, "automatic", "100000");
    private final VehicleTypeSearchResultDTO fortuner = car("Fortuner", "Toyota", 7, "manual", "300000");
    private final VehicleTypeSearchResultDTO mazda = car("Mazda3", "Mazda", 5, "automatic", "150000");
    private final List<VehicleTypeSearchResultDTO> all = List.of(vios, fortuner, mazda);

    @Test
    void noFilterKeepsEverythingInOrder() {
        assertThat(new RentalSearchCriteria(null, null, null, null, null).narrow(all)).containsExactly(vios, fortuner, mazda);
        assertThat(new RentalSearchCriteria("", "all", 0, "all", null).narrow(all)).containsExactly(vios, fortuner, mazda);
    }

    @Test
    void theKeywordMatchesNameBrandOrDisplayNameIgnoringCase() {
        assertThat(new RentalSearchCriteria("forTUN", "all", 0, "all", null).narrow(all)).containsExactly(fortuner);
        assertThat(new RentalSearchCriteria(" toyota ", "all", 0, "all", null).narrow(all)).containsExactly(vios, fortuner);
        assertThat(new RentalSearchCriteria("zzz", "all", 0, "all", null).narrow(all)).isEmpty();
    }

    @Test
    void brandAndTransmissionMustMatchExactlyButNotByCase() {
        assertThat(new RentalSearchCriteria(null, "mazda", 0, "all", null).narrow(all)).containsExactly(mazda);
        assertThat(new RentalSearchCriteria(null, "all", 0, "MANUAL", null).narrow(all)).containsExactly(fortuner);
    }

    @Test
    void seatsAreAMinimumAndAPriceIsAMaximum() {
        assertThat(new RentalSearchCriteria(null, "all", 7, "all", null).narrow(all)).containsExactly(fortuner);
        assertThat(new RentalSearchCriteria(null, "all", 0, "all", new BigDecimal("150000")).narrow(all)).containsExactly(vios, mazda);
    }

    @Test
    void aTypeWithoutSeatsFailsASeatsFilterButPassesWithoutOne() {
        VehicleTypeSearchResultDTO unknown = car("X", "Y", null, "automatic", "1");

        assertThat(new RentalSearchCriteria(null, "all", 4, "all", null).matches(unknown)).isFalse();
        assertThat(new RentalSearchCriteria(null, "all", 0, "all", null).matches(unknown)).isTrue();
    }

    @Test
    void filtersCombine() {
        assertThat(new RentalSearchCriteria("toyota", "toyota", 5, "automatic", new BigDecimal("120000")).narrow(all))
                .containsExactly(vios);
    }

    private static VehicleTypeSearchResultDTO car(String name, String brand, Integer seats, String transmission, String price) {
        VehicleTypeSearchResultDTO dto = new VehicleTypeSearchResultDTO();
        dto.setName(name);
        dto.setBrand(brand);
        dto.setSeats(seats);
        dto.setTransmission(transmission);
        dto.setPricePerHour(new BigDecimal(price));
        return dto;
    }
}
