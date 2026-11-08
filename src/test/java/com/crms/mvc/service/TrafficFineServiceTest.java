package com.crms.mvc.service;

import com.crms.mvc.dto.TrafficFineFormDTO;
import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.TrafficFineNotice;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.event.TrafficFineRecordedEvent;
import com.crms.mvc.event.TrafficFineRemovedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.TrafficFineNoticeRepository;
import com.crms.mvc.service.impl.TrafficFineServiceImpl;
import com.crms.mvc.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A traffic fine names a plate and a moment, not a booking. The plate may have moved to another car since, so the
 * car is the one that carried the plate at that moment, and the customer is whoever rented it then. Recording one
 * only tells that customer; nothing is charged.
 */
class TrafficFineServiceTest {
    private static final LocalDateTime VIOLATION = LocalDateTime.of(2026, 10, 20, 14, 30);

    private BookingRepository bookingRepository;
    private LicensePlateRepository plateRepository;
    private TrafficFineNoticeRepository noticeRepository;
    private EmployeeRepository employeeRepository;
    private FileUploadService fileUploadService;
    private ApplicationEventPublisher events;
    private TrafficFineService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        plateRepository = mock(LicensePlateRepository.class);
        noticeRepository = mock(TrafficFineNoticeRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        fileUploadService = mock(FileUploadService.class);
        events = mock(ApplicationEventPublisher.class);
        service = new TrafficFineServiceImpl(noticeRepository, bookingRepository, employeeRepository, plateRepository,
                mock(ContractService.class), fileUploadService, events);
    }

    // ---- lookup -----------------------------------------------------------------------------------------------

    @Test
    void theBookingIsTheRentalOfTheCarThatCarriedThePlateAtThatMoment() {
        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", null);
        Booking rental = new Booking();
        rental.setId(42L);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of(rental));

        Booking found = service.findBookingForViolation("51A-123.45", VIOLATION);

        assertSame(rental, found);
        verify(bookingRepository).findRentalsCovering(9L, VIOLATION.atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime());
    }

    @Test
    void thePlateIsMatchedWithoutCaseOrSurroundingSpaces() {
        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", null);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of(new Booking()));

        service.findBookingForViolation("  51a-123.45 ", VIOLATION);

        verify(plateRepository).findByPlateIgnoreCase("51a-123.45");
    }

    @Test
    void anUnknownPlateHasNoBooking() {
        when(plateRepository.findByPlateIgnoreCase("99Z-000.00")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("99Z-000.00", VIOLATION));

        verify(bookingRepository, never()).findRentalsCovering(any(), any());
    }

    @Test
    void aPlateThatWasNotOnTheCarYetOrWasAlreadyRetiredHasNoBooking() {
        plate("51A-123.45", vehicle(9L), "2026-11-01T00:00+07:00", null);
        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", "2026-10-01T00:00+07:00");
        assertThrows(NoSuchElementException.class, () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        verify(bookingRepository, never()).findRentalsCovering(any(), any());
    }

    @Test
    void aCarThatWasNotRentedAtThatMomentHasNoBooking() {
        plate("51A-123.45", vehicle(9L), "2026-01-01T00:00+07:00", null);
        when(bookingRepository.findRentalsCovering(eq(9L), any())).thenReturn(List.of());

        NoSuchElementException error = assertThrows(NoSuchElementException.class,
                () -> service.findBookingForViolation("51A-123.45", VIOLATION));

        assertTrue(error.getMessage().contains("Không có đơn thuê"));
    }

    // ---- recording and deleting ---------------------------------------------------------------------------------

    @Test
    void recordingSavesThePhotoAndTheNoticeAndTellsTheCustomer() {
        Booking rental = rental(42L);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(rental));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee()));
        when(fileUploadService.saveTrafficFineImage(any())).thenReturn("fine-1.jpg");
        when(noticeRepository.save(any(TrafficFineNotice.class))).thenAnswer(inv -> {
            TrafficFineNotice saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        TrafficFineNoticeDTO dto = service.record(form(42L), 2L);

        assertEquals(42L, dto.bookingId());
        assertEquals("51A-123.45", dto.plate());
        assertEquals("Nguyễn Văn A", dto.customerName());
        ArgumentCaptor<TrafficFineNotice> saved = ArgumentCaptor.forClass(TrafficFineNotice.class);
        verify(noticeRepository).save(saved.capture());
        assertEquals("fine-1.jpg", saved.getValue().getImageFileName());
        verify(events).publishEvent(any(TrafficFineRecordedEvent.class));
    }

    @Test
    void aFineNeedsThePhotoOfThePoliceNotice() {
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(rental(42L)));
        TrafficFineFormDTO form = form(42L);
        form.setImage(new MockMultipartFile("image", new byte[0]));

        assertThrows(IllegalArgumentException.class, () -> service.record(form, 2L));

        verify(noticeRepository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void aFineCannotBeRecordedForARentalTheCustomerNeverPickedUp() {
        Booking notPickedUp = rental(42L);
        notPickedUp.setActualPickupTime(null);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(notPickedUp));

        assertThrows(IllegalStateException.class, () -> service.record(form(42L), 2L));

        verify(noticeRepository, never()).save(any());
    }

    @Test
    void deletingARecordRemovesItsPhotoAndTellsTheCustomer() {
        TrafficFineNotice notice = new TrafficFineNotice();
        notice.setId(5L);
        notice.setBooking(rental(42L));
        notice.setPlate("51A-123.45");
        notice.setViolationAt(OffsetDateTime.now().minusDays(2));
        notice.setImageFileName("fine-1.jpg");
        notice.setRecordedBy(employee());
        notice.setRecordedAt(OffsetDateTime.now());
        when(noticeRepository.findWithDetailsById(5L)).thenReturn(Optional.of(notice));

        service.delete(5L);

        verify(noticeRepository).delete(notice);
        verify(fileUploadService).deleteTrafficFineImage("fine-1.jpg");
        verify(events).publishEvent(any(TrafficFineRemovedEvent.class));
    }

    @Test
    void aCustomerOnlySeesTheFinesOfTheirOwnBooking() {
        TrafficFineNotice notice = new TrafficFineNotice();
        notice.setId(5L);
        notice.setBooking(rental(42L));
        notice.setPlate("51A-123.45");
        notice.setViolationAt(OffsetDateTime.now().minusDays(2));
        notice.setRecordedBy(employee());
        notice.setRecordedAt(OffsetDateTime.now());
        when(noticeRepository.findByBookingIdOrderByViolationAtDesc(42L)).thenReturn(List.of(notice));

        assertEquals(1, service.findByBookingForCustomer(42L, 1L).size());
        assertTrue(service.findByBookingForCustomer(42L, 99L).isEmpty());
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    private TrafficFineFormDTO form(Long bookingId) {
        TrafficFineFormDTO form = new TrafficFineFormDTO();
        form.setBookingId(bookingId);
        form.setPlate("51a-123.45");
        form.setViolationAt(VIOLATION);
        form.setImage(new MockMultipartFile("image", "fine.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        return form;
    }

    private Booking rental(Long id) {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Nguyễn Văn A");
        customer.setEmail("a@example.com");
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setActualPickupTime(OffsetDateTime.now().minusDays(3));
        return booking;
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setName("Nhân viên");
        return employee;
    }

    private Vehicle vehicle(Long id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        return vehicle;
    }

    private void plate(String plate, Vehicle vehicle, String validFrom, String validTo) {
        LicensePlate row = new LicensePlate();
        row.setPlate(plate);
        row.setVehicle(vehicle);
        row.setValidFrom(OffsetDateTime.parse(validFrom));
        row.setValidTo(validTo == null ? null : OffsetDateTime.parse(validTo));
        when(plateRepository.findByPlateIgnoreCase(any())).thenReturn(Optional.of(row));
    }
}
