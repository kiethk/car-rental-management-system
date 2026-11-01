package com.crms.mvc.service;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.dto.SurchargeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.impl.HandoverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HandoverServiceTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private BookingFeeService bookingFeeService;
    @Mock private ContractService contractService;
    @Mock private PricingService pricingService;
    @Mock private PaymentService paymentService;
    @Mock private FileUploadService fileUploadService;
    @InjectMocks private HandoverServiceImpl handoverService;

    private Booking booking;
    private Vehicle vehicle;
    private Employee assigned;

    @BeforeEach
    void setUp() {
        assigned = employee(2L, "staff");
        Employee other = employee(3L, "staff");
        Employee admin = employee(1L, "admin");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(assigned));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(other));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(admin));

        vehicle = new Vehicle();
        vehicle.setOdometer(1000);
        vehicle.setStatus("available");

        booking = new Booking();
        booking.setId(10L);
        booking.setEmployee(assigned);
        booking.setVehicle(vehicle);
        booking.setStatus("confirmed");
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("300000"));
        booking.setPricePerHour(new BigDecimal("100000"));
        booking.setScheduledEndTime(OffsetDateTime.now().minusHours(1));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        when(issueRepository.findByBookingIdAndIssueType(eq(10L), anyString())).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenAnswer(call -> {
            Issue issue = call.getArgument(0);
            issue.setId(50L);
            return issue;
        });
        when(fileUploadService.saveHandoverImage(any())).thenReturn("photo.jpg");

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(new BigDecimal("1.5"));
        when(contractService.getTermsOfBooking(10L)).thenReturn(terms);
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(bookingFeeService.calculateTotalAdditionalFees(10L)).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void assignedStaffChecksInAndRecordIsSaved() {
        handoverService.checkIn(10L, 2L, checkInForm(1200));

        ArgumentCaptor<Issue> saved = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(saved.capture());
        assertEquals("CHECK_IN", saved.getValue().getIssueType());
        assertEquals("resolved", saved.getValue().getStatus());
        assertEquals("Handover: ODO 1200 km, fuel 80%, clean", saved.getValue().getSubject());
        assertEquals("photo.jpg", saved.getValue().getEvidenceUrl());
        assertEquals("photo.jpg", saved.getValue().getSecondaryEvidenceUrl());
        assertEquals("active", booking.getStatus());
        assertEquals("rented", vehicle.getStatus());
        assertEquals(1200, vehicle.getOdometer());
    }

    @Test
    void adminCheckInKeepsAssignedStaff() {
        handoverService.checkIn(10L, 1L, checkInForm(1200));

        assertEquals(assigned, booking.getEmployee());
        assertEquals("active", booking.getStatus());
    }

    @Test
    void otherStaffCannotCheckIn() {
        assertThrows(IllegalStateException.class, () -> handoverService.checkIn(10L, 3L, checkInForm(1200)));

        assertEquals("confirmed", booking.getStatus());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void lowerOdometerIsRejectedAtCheckIn() {
        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, checkInForm(999)));

        assertEquals("confirmed", booking.getStatus());
    }

    @Test
    void missingPhotoIsRejectedAtCheckIn() {
        CheckInDTO form = checkInForm(1200);
        form.setIdCardImage(new MockMultipartFile("idCardImage", new byte[0]));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, form));
        assertEquals("confirmed", booking.getStatus());
    }

    @Test
    void secondCheckInIsRejected() {
        when(issueRepository.findByBookingIdAndIssueType(10L, "CHECK_IN")).thenReturn(Optional.of(new Issue()));

        assertThrows(IllegalStateException.class, () -> handoverService.checkIn(10L, 2L, checkInForm(1200)));
    }

    @Test
    void checkOutRecordsLateFeeAndSurchargesAgainstTheRecord() {
        booking.setStatus("active");
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(new BigDecimal("150000"));
        CheckOutDTO form = checkOutForm(1300);
        form.setSurcharges(List.of(
                surcharge("CLEANING_FEE", "50000", "Interior cleaning"),
                surcharge("FUEL_FEE", null, null)));

        handoverService.checkOut(10L, 2L, form);

        verify(bookingFeeService).recordFee(10L, "LATE_FEE", new BigDecimal("150000"), "Late return fee", 50L);
        verify(bookingFeeService).recordFee(10L, "CLEANING_FEE", new BigDecimal("50000"), "Interior cleaning", 50L);
        verify(bookingFeeService, never()).recordFee(eq(10L), eq("FUEL_FEE"), any(), any(), any());
        assertEquals("active", booking.getStatus());
        assertEquals("available", vehicle.getStatus());
        assertEquals(1300, vehicle.getOdometer());
        assertTrue(booking.getActualReturnTime() != null);
    }

    @Test
    void damageSurchargeCreatesDamageRecordAndMaintenanceIsExplicit() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setNotes("hỏng nhẹ");
        form.setMoveToMaintenance(true);
        form.setSurcharges(List.of(surcharge("DAMAGE_COMPENSATION", "200000", "Scratched bumper")));

        handoverService.checkOut(10L, 2L, form);

        ArgumentCaptor<Issue> saved = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals("CHECK_OUT", saved.getAllValues().get(0).getIssueType());
        assertEquals("DAMAGE", saved.getAllValues().get(1).getIssueType());
        assertEquals("maintenance", vehicle.getStatus());
    }

    @Test
    void notesAloneDoNotMoveVehicleToMaintenance() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setNotes("hỏng nhẹ");

        handoverService.checkOut(10L, 2L, form);

        assertEquals("available", vehicle.getStatus());
    }

    @Test
    void unknownOrForgedSurchargeTypeIsRejected() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setSurcharges(List.of(surcharge("LATE_FEE", "1000", "forged")));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkOut(10L, 2L, form));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void otherStaffCannotCheckOutOrSettle() {
        booking.setStatus("active");

        assertThrows(IllegalStateException.class, () -> handoverService.checkOut(10L, 3L, checkOutForm(1300)));
        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 3L, new SettlementDTO()));
    }

    @Test
    void secondCheckOutIsRejected() {
        booking.setStatus("active");
        when(issueRepository.findByBookingIdAndIssueType(10L, "CHECK_OUT")).thenReturn(Optional.of(new Issue()));

        assertThrows(IllegalStateException.class, () -> handoverService.checkOut(10L, 2L, checkOutForm(1300)));
    }

    @Test
    void settlementCollectsBalanceAtCounterAndKeepsRentalTotal() {
        Issue record = returnedBooking();
        when(bookingFeeService.calculateTotalAdditionalFees(10L)).thenReturn(new BigDecimal("100000"));
        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        handoverService.settle(10L, 2L, form);

        verify(paymentService).recordFinalPayment(10L, new BigDecimal("800000"), "CASH");
        assertEquals("completed", booking.getStatus());
        assertEquals("resolved", record.getStatus());
        assertEquals(new BigDecimal("1000000"), booking.getTotalPrice());
    }

    @Test
    void settlementRequiresPaymentMethodWhenCustomerOwes() {
        returnedBooking();

        assertThrows(IllegalArgumentException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));
        assertEquals("active", booking.getStatus());
    }

    @Test
    void settlementWithZeroBalanceCreatesNoPayment() {
        returnedBooking();
        booking.setDepositAmount(new BigDecimal("1000000"));

        handoverService.settle(10L, 2L, new SettlementDTO());

        verify(paymentService, never()).recordFinalPayment(any(), any(), any());
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        assertEquals("completed", booking.getStatus());
    }

    @Test
    void settlementCreatesPendingRefundWhenDepositExceedsTotal() {
        returnedBooking();
        booking.setDepositAmount(new BigDecimal("1200000"));
        SettlementDTO form = new SettlementDTO();
        form.setBankCode("VCB");
        form.setAccountNumber("123456");
        form.setAccountHolder("NGUYEN VAN A");

        handoverService.settle(10L, 2L, form);

        verify(paymentService).createPendingRefund(10L, new BigDecimal("200000"), "VCB", "123456", "NGUYEN VAN A");
        assertEquals("completed", booking.getStatus());
    }

    @Test
    void refundRequiresBankDetails() {
        returnedBooking();
        booking.setDepositAmount(new BigDecimal("1200000"));

        assertThrows(IllegalArgumentException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));
        assertEquals("active", booking.getStatus());
    }

    @Test
    void settlementBeforeReturnOrAfterCompletionIsRejected() {
        booking.setStatus("active");
        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));

        booking.setStatus("completed");
        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));
    }

    @Test
    void lockedEmployeeCannotBeAssigned() {
        Employee locked = employee(4L, "staff");
        locked.setActive(false);
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(locked));

        assertThrows(IllegalArgumentException.class, () -> handoverService.reassignStaff(10L, 4L));
        assertEquals(assigned, booking.getEmployee());
    }

    @Test
    void evidenceOfAnotherBookingIsNotServed() {
        Booking otherBooking = new Booking();
        otherBooking.setId(99L);
        Issue issue = new Issue();
        issue.setBooking(otherBooking);
        issue.setEvidenceUrl("photo.jpg");
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));

        assertThrows(java.util.NoSuchElementException.class, () -> handoverService.loadEvidence(10L, 7L, false));
    }

    private Issue returnedBooking() {
        booking.setStatus("active");
        Issue record = new Issue();
        record.setStatus("open");
        when(issueRepository.findByBookingIdAndIssueType(10L, "CHECK_OUT")).thenReturn(Optional.of(record));
        return record;
    }

    private Employee employee(Long id, String role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setRole(role);
        return employee;
    }

    private CheckInDTO checkInForm(int odometer) {
        CheckInDTO form = new CheckInDTO();
        form.setOdometer(odometer);
        form.setFuelPercent(80);
        form.setNotes("clean");
        form.setDocumentsVerified(true);
        form.setLicenseImage(new MockMultipartFile("licenseImage", "l.jpg", "image/jpeg", new byte[]{1}));
        form.setIdCardImage(new MockMultipartFile("idCardImage", "i.jpg", "image/jpeg", new byte[]{1}));
        return form;
    }

    private CheckOutDTO checkOutForm(int odometer) {
        CheckOutDTO form = new CheckOutDTO();
        form.setOdometer(odometer);
        form.setFuelPercent(60);
        return form;
    }

    private SurchargeDTO surcharge(String type, String amount, String description) {
        SurchargeDTO surcharge = new SurchargeDTO();
        surcharge.setFeeType(type);
        surcharge.setAmount(amount != null ? new BigDecimal(amount) : null);
        surcharge.setDescription(description);
        return surcharge;
    }
}
