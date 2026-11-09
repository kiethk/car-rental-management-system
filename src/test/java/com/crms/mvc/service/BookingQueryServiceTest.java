package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.config.StationProperties;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.impl.BookingDetailMapper;
import com.crms.mvc.service.impl.BookingQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reading bookings: who may open one, and how the customer's and the admin's lists are put together. */
class BookingQueryServiceTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private BookingQueryService service;

    @BeforeEach
    void setUp() {
        BookingDetailMapper mapper = new BookingDetailMapper(payments, mock(ContractService.class),
                mock(BookingFeeService.class), mock(PricingService.class), StationProperties.DEFAULT);
        service = new BookingQueryServiceImpl(bookings, payments, mapper);
    }

    @Test
    void aBookingCanOnlyBeOpenedByItsOwner() {
        when(bookings.findById(42L)).thenReturn(Optional.of(booking(42L, 7L, BookingStatus.CONFIRMED)));

        assertThatThrownBy(() -> service.getBookingDetail(42L, 8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void anUnknownBookingIsNotFound() {
        assertThatThrownBy(() -> service.getBookingDetail(99L, 7L)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.getBookingDetailForAdmin(99L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void theOwnerAndTheAdminSeeTheSameDetail() {
        when(bookings.findById(42L)).thenReturn(Optional.of(booking(42L, 7L, BookingStatus.CONFIRMED)));

        assertThat(service.getBookingDetail(42L, 7L).getId()).isEqualTo(42L);
        assertThat(service.getBookingDetail(42L, null).getStatus()).isEqualTo("confirmed");
        assertThat(service.getBookingDetailForAdmin(42L).getCustomerId()).isEqualTo(7L);
        assertThat(service.getBookingDetailForAdmin(42L).getStationAddress()).isEqualTo(StationProperties.DEFAULT_ADDRESS);
    }

    @Test
    void aBookingWithoutACarShowsAsUnassigned() {
        when(bookings.findByCustomerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(booking(42L, 7L, BookingStatus.CONFIRMED)));

        List<BookingSummaryDTO> mine = service.getMyBookings(7L);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getLicensePlate()).isEqualTo("Unassigned");
        assertThat(mine.get(0).getStatus()).isEqualTo("confirmed");
        assertThat(mine.get(0).getVehicleTypeName()).isEqualTo("Vios");
    }

    @Test
    void theRefundFilterListsBookingsWaitingForARefundAndMarksThem() {
        Booking waiting = booking(42L, 7L, BookingStatus.CANCELLED);
        when(payments.getPendingRefundBookingIds()).thenReturn(List.of(42L));
        when(bookings.findBookingsWithPendingRefund()).thenReturn(List.of(waiting));

        List<BookingSummaryDTO> list = service.getAllBookings("refund");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("pending_refund");
        assertThat(list.get(0).isHasPendingRefund()).isTrue();
        assertThat(list.get(0).getRefundAmount()).isEqualByComparingTo("300000");
    }

    @Test
    void noFilterListsEveryBookingAndAStatusFilterIsTrimmed() {
        when(payments.getPendingRefundBookingIds()).thenReturn(List.of());
        when(bookings.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(booking(1L, 7L, BookingStatus.ACTIVE)));
        when(bookings.findByStatusOrderByCreatedAtDesc(BookingStatus.ACTIVE)).thenReturn(List.of(booking(2L, 7L, BookingStatus.ACTIVE)));

        assertThat(service.getAllBookings(null)).extracting(BookingSummaryDTO::getId).containsExactly(1L);
        assertThat(service.getAllBookings("all")).extracting(BookingSummaryDTO::getId).containsExactly(1L);
        assertThat(service.getAllBookings("  active ")).extracting(BookingSummaryDTO::getId).containsExactly(2L);
        verify(bookings, never()).findBookingsWithPendingRefund();
    }

    private static Booking booking(Long id, Long customerId, BookingStatus status) {
        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setName("Vios");
        type.setBrand("Toyota");
        Slot slot = new Slot();
        slot.setId(5L);
        slot.setVehicleType(type);
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Nguyễn Văn An");
        Booking b = new Booking();
        b.setId(id);
        b.setCustomer(customer);
        b.setSlot(slot);
        b.setStatus(status);
        b.setDepositAmount(new BigDecimal("300000"));
        b.setTotalPrice(new BigDecimal("1000000"));
        b.setPickupTime(OffsetDateTime.now().plusDays(1));
        b.setScheduledEndTime(OffsetDateTime.now().plusDays(2));
        return b;
    }
}
