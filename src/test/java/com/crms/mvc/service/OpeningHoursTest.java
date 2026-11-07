package com.crms.mvc.service;

import com.crms.mvc.config.OpeningHours;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Pickup and return happen inside the depot's opening hours, checked on the server, not only in the browser. */
class OpeningHoursTest {

    private final LocalDateTime day = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.DAYS);

    @Test
    void openingAndClosingHoursAreBothInside() {
        OpeningHours hours = new OpeningHours(6, 22);
        assertThat(hours.contains(day.withHour(6))).isTrue();
        assertThat(hours.contains(day.withHour(22))).isTrue();
        assertThat(hours.contains(day.withHour(5))).isFalse();
        assertThat(hours.contains(day.withHour(23))).isFalse();
        assertThat(hours.getLabel()).isEqualTo("06:00 - 22:00");
    }

    @Test
    void aPickupAtThreeInTheMorningIsRefusedByTheServer() {
        BookingServiceImpl service = new BookingServiceImpl(mock(BookingRepository.class), mock(CustomerRepository.class),
                mock(SlotRepository.class), mock(VehicleTypeRepository.class), mock(VehicleRepository.class),
                mock(EmployeeRepository.class), mock(PromotionRepository.class), mock(PaymentService.class),
                mock(ContractService.class), mock(ContractTemplateService.class), mock(BookingFeeService.class),
                mock(PricingService.class), mock(CompensationVoucherService.class));

        assertThatThrownBy(() -> service.validateRentalSchedule(day.withHour(3), day.withHour(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giờ nhận xe phải trong giờ mở cửa");
        assertThatThrownBy(() -> service.validateRentalSchedule(day.withHour(10), day.withHour(23)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giờ trả xe phải trong giờ mở cửa");
        assertThatCode(() -> service.validateRentalSchedule(day.withHour(6), day.withHour(22))).doesNotThrowAnyException();
    }
}
