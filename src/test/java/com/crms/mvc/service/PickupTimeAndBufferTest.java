package com.crms.mvc.service;

import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.entity.*;
import com.crms.mvc.enums.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.service.impl.VehicleAvailabilityServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupTimeAndBufferTest {

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
    @Mock
    private HandoverRepository handoverRepository;

    private HandoverServiceImpl handoverService;
    private IssueServiceImpl issueService;

    private Booking booking;
    private Vehicle vehicle;
    private Employee employee;

    @BeforeEach
    void setUp() {
        handoverService = new HandoverServiceImpl(
                bookingRepository, employeeRepository, vehicleRepository,
                issueRepository, handoverRepository, bookingFeeService, contractService,
                pricingService, paymentService, fileUploadService);

        issueService = new IssueServiceImpl(
                issueRepository, bookingRepository, employeeRepository,
                bookingFeeRepository, mock(IssueEvidenceRepository.class),
                fileUploadService, licensePlateRepository, mock(PaymentRepository.class),
                eventPublisher);

        employee = new Employee();
        employee.setId(1L);
        employee.setName("Staff Member");
        employee.setRole(EmployeeRole.STAFF);
        employee.setActive(true);

        vehicle = new Vehicle();
        vehicle.setId(100L);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setOdometer(5000);

        com.crms.mvc.entity.Customer customer = new com.crms.mvc.entity.Customer();
        customer.setId(5L);

        booking = new Booking();
        booking.setId(10L);
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setEmployee(employee);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPickupTime(OffsetDateTime.now().plusHours(1)); // scheduled 1 hour in future
        booking.setScheduledEndTime(OffsetDateTime.now().plusDays(1));
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("300000"));
        booking.setPricePerHour(new BigDecimal("100000"));

        lenient().when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        lenient().when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        lenient().when(fileUploadService.saveHandoverImage(any())).thenReturn("photo.jpg");
        lenient().when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of());
    }

    private CheckInDTO createCheckInForm() {
        CheckInDTO form = new CheckInDTO();
        form.setOdometer(5010);
        form.setFuelPercent(100);
        form.setNotes("Sẵn sàng giao xe");
        form.setPaymentMethod("CASH");
        form.setPaymentReceived(true);
        form.setLicenseImage(new MockMultipartFile("license", "lic.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        form.setIdCardImage(new MockMultipartFile("idCard", "id.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        return form;
    }

    @Test
    @DisplayName("Check-in rejected before the scheduled pickup time: there is no early pickup")
    void testCheckInRejectedBeforeScheduledPickup() {
        booking.setPickupTime(OffsetDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> handoverService.checkIn(10L, 1L, createCheckInForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chưa đến giờ nhận xe");
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    @DisplayName("Check-in succeeds from the scheduled pickup time when the car is available")
    void testCheckInSucceedsFromScheduledPickup() {
        booking.setPickupTime(OffsetDateTime.now().minusMinutes(1));

        handoverService.checkIn(10L, 1L, createCheckInForm());

        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
        assertNotNull(booking.getActualPickupTime());
    }

    @Test
    @DisplayName("Check-in rejected when the assigned car is still out: staff must reassign another car")
    void testCheckInRejectedWhenAssignedCarNotAvailable() {
        booking.setPickupTime(OffsetDateTime.now().minusMinutes(1));
        vehicle.setStatus(VehicleStatus.RENTED);

        assertThatThrownBy(() -> handoverService.checkIn(10L, 1L, createCheckInForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chưa sẵn sàng bàn giao")
                .hasMessageContaining("đổi sang xe khác cùng loại");
    }

    @Test
    @DisplayName("Customer liability starts at the actual pickup, not the scheduled one: a car handed over late")
    void testIssueBeforeActualPickupIsRejected() {
        // Scheduled 60 minutes ago, but the car was only handed over 10 minutes ago
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setPickupTime(OffsetDateTime.now().minusMinutes(60));
        booking.setActualPickupTime(OffsetDateTime.now().minusMinutes(10));

        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(10L);
        form.setIssueType(IssueType.COLLISION);
        form.setSubject("Va quẹt nhẹ gương chiếu hậu");
        form.setFeeAmount(new BigDecimal("500000"));
        // 30 minutes ago: after the scheduled pickup, but the customer did not have the car yet
        form.setOccurredAt(LocalDateTime.now(DateTimeUtils.VIETNAM_ZONE).minusMinutes(30));

        assertThatThrownBy(() -> issueService.create(form, new IssueActor(1L, "staff")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được trước thời gian nhận xe");
    }

    @Test
    @DisplayName("Settlement ties the unpaid damage fee to the settlement payment, which resolves its issue")
    void testSettlementCollectsOpenDamageIssueFees() {
        booking.setStatus(BookingStatus.ACTIVE);

        Handover checkOutRecord = new Handover();
        checkOutRecord.setType(HandoverType.CHECK_OUT);
        when(handoverRepository.findByBookingIdAndType(10L, HandoverType.CHECK_OUT)).thenReturn(Optional.of(checkOutRecord));

        BookingFee damageFee = new BookingFee();
        damageFee.setAmount(new BigDecimal("200000"));
        List<BookingFee> unpaid = List.of(damageFee);
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(unpaid);
        Payment finalPayment = new Payment();
        when(paymentService.recordFinalPayment(10L, new BigDecimal("200000"), PaymentMethod.CASH)).thenReturn(finalPayment);

        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        handoverService.settle(10L, 1L, form);

        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertNotNull(checkOutRecord.getSettledAt());
        verify(bookingFeeService).attachPayment(unpaid, finalPayment);
    }

    @Test
    @DisplayName("Symmetrical 1-hour buffer: booking at 17:00 requires preceding booking to end by 16:00")
    void testPrecedingBookingMustEndOneHourBeforeNextBooking() {
        VehicleAvailabilityServiceImpl availability = new VehicleAvailabilityServiceImpl(bookingRepository, vehicleRepository,
                mock(VehicleTypeRepository.class), mock(ContractTemplateService.class), pricingService);

        Vehicle car = new Vehicle();
        car.setId(100L);
        car.setStatus(VehicleStatus.AVAILABLE);
        when(vehicleRepository.findByVehicleTypeIdWithDetails(1L)).thenReturn(List.of(car));

        // Given: Existing booking at 17:00 - 19:00
        OffsetDateTime fivePm = OffsetDateTime.now().withHour(17).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime morningStart = fivePm.minusHours(8); // 09:00

        // Case 1: Candidate wants 09:00 - 16:30 (only 30 mins before 17:00)
        OffsetDateTime invalidEnd = fivePm.minusMinutes(30); // 16:30
        // When searching, findBookedVehicleIdsInTimeRange is called with bufferedEnd = 16:30 + 1h = 17:30
        // which overlaps with existing booking starting at 17:00 (< 17:30), so DB returns [100L]
        when(bookingRepository.findBookedVehicleIdsInTimeRange(
                eq(morningStart.minusHours(1)), eq(invalidEnd.plusHours(1)), any(), any()))
                .thenReturn(List.of(100L));

        List<Vehicle> availableInvalid = availability.findAvailableVehiclesInTimeRange(1L, morningStart, invalidEnd);
        assertThat(availableInvalid).isEmpty(); // Blocked! Cannot book ending at 16:30

        // Case 2: Candidate wants 09:00 - 16:00 (ends exactly 1 hour before 17:00)
        OffsetDateTime validEnd = fivePm.minusHours(1); // 16:00
        // When searching, bufferedEnd = 16:00 + 1h = 17:00. DB condition pickupTime (17:00) < 17:00 is false!
        when(bookingRepository.findBookedVehicleIdsInTimeRange(
                eq(morningStart.minusHours(1)), eq(validEnd.plusHours(1)), any(), any()))
                .thenReturn(List.of()); // No conflict

        List<Vehicle> availableValid = availability.findAvailableVehiclesInTimeRange(1L, morningStart, validEnd);
        assertThat(availableValid).containsExactly(car); // Allowed!
    }
}
