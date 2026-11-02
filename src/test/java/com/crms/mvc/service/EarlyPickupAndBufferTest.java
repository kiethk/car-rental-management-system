package com.crms.mvc.service;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.entity.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.impl.BookingServiceImpl;
import com.crms.mvc.service.impl.HandoverServiceImpl;
import com.crms.mvc.service.impl.IssueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarlyPickupAndBufferTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private BookingFeeService bookingFeeService;
    @Mock
    private ContractService contractService;
    @Mock
    private PricingService pricingService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private LicensePlateRepository licensePlateRepository;
    @Mock
    private BookingFeeRepository bookingFeeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HandoverServiceImpl handoverService;
    private IssueServiceImpl issueService;

    private Booking booking;
    private Vehicle vehicle;
    private Employee employee;

    @BeforeEach
    void setUp() {
        handoverService = new HandoverServiceImpl(
                bookingRepository, employeeRepository, vehicleRepository,
                issueRepository, bookingFeeService, contractService,
                pricingService, paymentService, fileUploadService);

        issueService = new IssueServiceImpl(
                issueRepository, bookingRepository, employeeRepository,
                bookingFeeRepository, mock(IssueEvidenceRepository.class),
                fileUploadService, licensePlateRepository, mock(PaymentRepository.class),
                eventPublisher);

        employee = new Employee();
        employee.setId(1L);
        employee.setName("Staff Member");
        employee.setRole("staff");
        employee.setActive(true);

        vehicle = new Vehicle();
        vehicle.setId(100L);
        vehicle.setStatus("available");
        vehicle.setOdometer(5000);

        booking = new Booking();
        booking.setId(10L);
        booking.setVehicle(vehicle);
        booking.setEmployee(employee);
        booking.setStatus("confirmed");
        booking.setPickupTime(OffsetDateTime.now().plusHours(1)); // scheduled 1 hour in future
        booking.setScheduledEndTime(OffsetDateTime.now().plusDays(1));
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("300000"));
        booking.setPricePerHour(new BigDecimal("100000"));

        lenient().when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        lenient().when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        lenient().when(fileUploadService.saveHandoverImage(any())).thenReturn("photo.jpg");
        lenient().when(bookingFeeService.calculateTotalAdditionalFees(10L)).thenReturn(BigDecimal.ZERO);
    }

    private CheckInDTO createCheckInForm() {
        CheckInDTO form = new CheckInDTO();
        form.setOdometer(5010);
        form.setFuelPercent(100);
        form.setNotes("Sẵn sàng giao xe");
        form.setLicenseImage(new MockMultipartFile("license", "lic.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        form.setIdCardImage(new MockMultipartFile("idCard", "id.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        return form;
    }

    @Test
    @DisplayName("Check-in rejected when customer arrives more than 30 minutes before scheduled pickup")
    void testCheckInRejectedWhenArrivingTooEarly() {
        // Scheduled in 2 hours (> 30 mins)
        booking.setPickupTime(OffsetDateTime.now().plusHours(2));

        assertThatThrownBy(() -> handoverService.checkIn(10L, 1L, createCheckInForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chỉ được phép nhận xe trước giờ hẹn tối đa 30 phút");
    }

    @Test
    @DisplayName("Check-in rejected when arriving early (<30m) but vehicle is not available (still being cleaned)")
    void testCheckInRejectedWhenEarlyAndVehicleNotAvailable() {
        // Scheduled in 20 minutes (within 30 mins)
        booking.setPickupTime(OffsetDateTime.now().plusMinutes(20));
        // Vehicle is still in maintenance or rented by prior slot
        vehicle.setStatus("maintenance");

        assertThatThrownBy(() -> handoverService.checkIn(10L, 1L, createCheckInForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Phương tiện hiện đang trong quy trình kiểm tra và vệ sinh sau ca thuê trước")
                .hasMessageContaining("đổi sang phương tiện cùng loại khác biển số");
    }

    @Test
    @DisplayName("Check-in succeeds when arriving early (<30m) and vehicle is available")
    void testCheckInSucceedsWhenEarlyAndVehicleAvailable() {
        // Scheduled in 15 minutes, car is clean & available
        booking.setPickupTime(OffsetDateTime.now().plusMinutes(15));
        vehicle.setStatus("available");

        handoverService.checkIn(10L, 1L, createCheckInForm());

        assertEquals("active", booking.getStatus());
        assertEquals("rented", vehicle.getStatus());
        assertNotNull(booking.getActualPickupTime());
        assertThat(booking.getActualPickupTime()).isBefore(booking.getPickupTime());
    }

    @Test
    @DisplayName("Issue can be created for actual early pickup time even if before scheduled pickup time")
    void testIssueCreationAllowedAfterActualPickupTime() {
        // Scheduled at 11:00, but picked up at 10:35
        OffsetDateTime scheduled = OffsetDateTime.now().plusMinutes(25);
        OffsetDateTime actualPickup = OffsetDateTime.now().minusMinutes(10);
        booking.setStatus("active");
        booking.setPickupTime(scheduled);
        booking.setActualPickupTime(actualPickup);

        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(10L);
        form.setIssueType(IssueType.COLLISION);
        form.setSubject("Va quẹt nhẹ gương chiếu hậu");
        form.setFeeAmount(new BigDecimal("500000"));
        // Incident occurred 5 minutes ago (between actualPickup and scheduled pickup)
        form.setOccurredAt(LocalDateTime.now().minusMinutes(5));

        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue created = issueService.create(form, new IssueActor(1L, "staff"));
        assertNotNull(created);
        assertEquals(IssueStatus.OPEN, created.getStatus());
        assertEquals("COLLISION", created.getIssueType().name());
    }

    @Test
    @DisplayName("Settlement automatically marks open damage issues as RESOLVED")
    void testSettlementResolvesOpenDamageIssues() {
        booking.setStatus("active");

        Issue checkOutRecord = new Issue();
        checkOutRecord.setId(201L);
        checkOutRecord.setStatus("open");
        when(issueRepository.findByBookingIdAndIssueType(10L, "CHECK_OUT")).thenReturn(Optional.of(checkOutRecord));

        Issue damageIssue = new Issue();
        damageIssue.setId(301L);
        damageIssue.setStatus(IssueStatus.OPEN);

        List<Issue> bookingIssues = new ArrayList<>();
        bookingIssues.add(checkOutRecord);
        bookingIssues.add(damageIssue);
        when(issueRepository.findByBookingIdOrderByReportedAtAsc(10L)).thenReturn(bookingIssues);

        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        handoverService.settle(10L, 1L, form);

        assertEquals("completed", booking.getStatus());
        assertEquals(IssueStatus.RESOLVED, damageIssue.getStatus());
    }
}
