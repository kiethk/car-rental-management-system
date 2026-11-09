package com.crms.mvc.service;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.ReturnChargeDTO;
import com.crms.mvc.dto.ReturnReceiptDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Handover;
import com.crms.mvc.entity.HandoverType;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.HandoverRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock private HandoverRepository handoverRepository;
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

        when(handoverRepository.existsByBookingIdAndType(eq(10L), any())).thenReturn(false);
        when(handoverRepository.save(any(Handover.class))).thenAnswer(call -> call.getArgument(0));
        when(fileUploadService.saveHandoverImage(any())).thenReturn("photo.jpg");

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(new BigDecimal("1.5"));
        when(contractService.getTermsOfBooking(10L)).thenReturn(terms);
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of());
        when(bookingFeeService.calculatePrepaidIssueFees(10L)).thenReturn(BigDecimal.ZERO);

    }

    @Test
    void assignedStaffChecksInAndHandoverIsSaved() {
        handoverService.checkIn(10L, 2L, checkInForm(1200));

        ArgumentCaptor<Handover> saved = ArgumentCaptor.forClass(Handover.class);
        verify(handoverRepository).save(saved.capture());
        assertEquals(HandoverType.CHECK_IN, saved.getValue().getType());
        assertEquals(1200, saved.getValue().getOdometer());
        assertEquals(80, saved.getValue().getFuelPercent());
        assertEquals("clean", saved.getValue().getNotes());
        assertEquals("photo.jpg", saved.getValue().getLicenseImageUrl());
        assertEquals("photo.jpg", saved.getValue().getIdCardImageUrl());
        assertEquals("active", booking.getStatus());
        assertEquals("rented", vehicle.getStatus());
        assertEquals(1200, vehicle.getOdometer());
        verify(issueRepository, never()).save(any());
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
        verify(handoverRepository, never()).save(any());
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
        when(handoverRepository.existsByBookingIdAndType(10L, HandoverType.CHECK_IN)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> handoverService.checkIn(10L, 2L, checkInForm(1200)));
    }

    @Test
    void checkOutRecordsLateFeeAndReturnChargesAgainstTheBookingOnly() {
        booking.setStatus("active");
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(new BigDecimal("150000"));
        CheckOutDTO form = checkOutForm(1300);
        form.setReturnCharges(List.of(
                returnCharge("CLEANING_FEE", "50000", "Interior cleaning"),
                returnCharge("TOLL_FEE", null, null)));

        handoverService.checkOut(10L, 2L, form);

        verify(bookingFeeService).recordFee(10L, "LATE_FEE", new BigDecimal("150000"), "Phí trả xe trễ", null);
        verify(bookingFeeService).recordFee(10L, "CLEANING_FEE", new BigDecimal("50000"), "Interior cleaning", null);
        verify(bookingFeeService, never()).recordFee(eq(10L), eq("TOLL_FEE"), any(), any(), any());
        ArgumentCaptor<Handover> saved = ArgumentCaptor.forClass(Handover.class);
        verify(handoverRepository).save(saved.capture());
        assertEquals(HandoverType.CHECK_OUT, saved.getValue().getType());
        assertEquals("active", booking.getStatus());
        assertEquals("available", vehicle.getStatus());
        assertEquals(1300, vehicle.getOdometer());
        assertNotNull(booking.getActualReturnTime());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void damageIsNotAReturnCharge() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setReturnCharges(List.of(returnCharge("DAMAGE_COMPENSATION", "200000", "Scratched bumper")));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkOut(10L, 2L, form));
        verify(handoverRepository, never()).save(any());
    }

    @Test
    void maintenanceIsWhateverStaffTicked() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setMoveToMaintenance(true);

        handoverService.checkOut(10L, 2L, form);

        assertEquals("maintenance", vehicle.getStatus());
    }

    @Test
    void unknownOrForgedReturnChargeTypeIsRejected() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setReturnCharges(List.of(returnCharge("LATE_FEE", "1000", "forged")));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkOut(10L, 2L, form));
        verify(handoverRepository, never()).save(any());
    }

    @Test
    void fuelIsNeverCharged() {
        booking.setStatus("active");
        CheckOutDTO form = checkOutForm(1300);
        form.setReturnCharges(List.of(returnCharge("FUEL_FEE", "100000", "Refuel")));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkOut(10L, 2L, form));
        verify(handoverRepository, never()).save(any());
        verify(bookingFeeService, never()).recordFee(any(), any(), any(), any(), any());
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
        when(handoverRepository.existsByBookingIdAndType(10L, HandoverType.CHECK_OUT)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> handoverService.checkOut(10L, 2L, checkOutForm(1300)));
    }

    @Test
    void settlementCollectsOnlyUnpaidFeesAndTiesThemToTheFinalPayment() {
        Handover record = returnedBooking();
        List<BookingFee> unpaid = List.of(fee("100000"));
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(unpaid);
        Payment finalPayment = new Payment();
        when(paymentService.recordFinalPayment(10L, new BigDecimal("800000"), "CASH")).thenReturn(finalPayment);
        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        handoverService.settle(10L, 2L, form);

        verify(bookingFeeService).attachPayment(unpaid, finalPayment);
        assertEquals("completed", booking.getStatus());
        assertTrue(record.isSettled());
        assertEquals(new BigDecimal("1000000"), booking.getTotalPrice());
    }

    @Test
    void settlementIsBlockedWhileTheCustomerHasALiveIssuePaymentSession() {
        returnedBooking();
        when(paymentService.hasLiveIssuePaymentSession(10L)).thenReturn(true);
        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 2L, form));
        verify(paymentService, never()).recordFinalPayment(any(), any(), any());
        assertEquals("active", booking.getStatus());
    }

    @Test
    void settlementRequiresPaymentMethodWhenCustomerOwes() {
        returnedBooking();

        assertThrows(IllegalArgumentException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));
        assertEquals("active", booking.getStatus());
    }

    @Test
    void fullDepositAndNoFeesClosesTheBookingWithoutAnyPayment() {
        returnedBooking();
        booking.setDepositAmount(new BigDecimal("1000000"));

        handoverService.settle(10L, 2L, new SettlementDTO());

        verify(paymentService, never()).recordFinalPayment(any(), any(), any());
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        verify(bookingFeeService, never()).attachPayment(anyList(), any());
        assertEquals("completed", booking.getStatus());
    }

    @Test
    void feesAreNeverTakenOutOfTheDeposit() {
        returnedBooking();
        booking.setDepositAmount(new BigDecimal("1000000"));
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of(fee("100000")));

        // Deposit already equals the rent, yet the fee is still owed at the counter.
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
    void summaryShowsUnpaidAndPrepaidFeesSeparately() {
        returnedBooking();
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of(fee("100000")));
        when(bookingFeeService.calculatePrepaidIssueFees(10L)).thenReturn(new BigDecimal("500000"));
        when(issueRepository.existsByBookingIdAndIssueTypeInAndStatusNot(eq(10L), any(), any())).thenReturn(true);

        HandoverSummaryDTO summary = handoverService.getSummary(10L);

        assertEquals(new BigDecimal("100000"), summary.getFeesTotal());
        assertEquals(new BigDecimal("500000"), summary.getPrepaidTotal());
        assertEquals(new BigDecimal("800000"), summary.getFinalAmount());
        assertTrue(summary.isReturned());
        assertFalse(summary.isSettled());
        assertTrue(summary.isVehicleDamageReported());
    }

    @Test
    void negativeFinalAmountBlocksSettlement() {
        Handover record = returnedBooking();
        booking.setDepositAmount(new BigDecimal("1200000"));
        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 2L, form));
        verify(paymentService, never()).recordFinalPayment(any(), any(), any());
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        assertEquals("active", booking.getStatus());
        assertFalse(record.isSettled());
    }

    @Test
    void noReturnReceiptBeforeTheVehicleIsReturned() {
        when(handoverRepository.findByBookingIdAndType(10L, HandoverType.CHECK_OUT)).thenReturn(Optional.empty());

        assertNull(handoverService.getReturnReceipt(10L));
    }

    @Test
    void returnReceiptBeforeSettlementShowsWhatIsStillOwed() {
        returnedBooking();
        booking.setActualReturnTime(OffsetDateTime.now());
        BookingFee late = fee("150000");
        late.setDescription("Late return fee");
        BookingFee prepaid = fee("500000");
        prepaid.setDescription("Scratch");
        prepaid.setPayment(payment(Payment.TYPE_ISSUE_COMPENSATION, Payment.STATUS_PAID, "500000", "VNPAY"));
        when(bookingFeeService.getFeesByBooking(10L)).thenReturn(List.of(late, prepaid));
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of(late));

        ReturnReceiptDTO receipt = handoverService.getReturnReceipt(10L);

        assertFalse(receipt.isSettled());
        assertEquals(1300, receipt.getOdometer());
        assertEquals(60, receipt.getFuelPercent());
        assertEquals(2, receipt.getLines().size());
        assertFalse(receipt.getLines().get(0).isPaidEarlier());
        assertTrue(receipt.getLines().get(1).isPaidEarlier());
        assertEquals(new BigDecimal("850000"), receipt.getAmountDue());
    }

    @Test
    void returnReceiptAfterSettlementShowsTheFinalPayment() {
        Handover record = returnedBooking();
        record.setSettledAt(OffsetDateTime.now());
        BookingFee late = fee("150000");
        Payment finalPayment = payment(Payment.TYPE_FINAL, Payment.STATUS_PAID, "850000", "CASH");
        late.setPayment(finalPayment);
        when(bookingFeeService.getFeesByBooking(10L)).thenReturn(List.of(late));
        when(paymentService.getPaymentsByBooking(10L)).thenReturn(List.of(
                payment(Payment.TYPE_DEPOSIT, Payment.STATUS_PAID, "300000", "VNPAY"), finalPayment));

        ReturnReceiptDTO receipt = handoverService.getReturnReceipt(10L);

        assertTrue(receipt.isSettled());
        assertFalse(receipt.getLines().get(0).isPaidEarlier());
        assertEquals(new BigDecimal("850000"), receipt.getAmountDue());
        assertEquals("Tiền mặt", receipt.getPaymentMethodLabel());
        assertNotNull(receipt.getPaidAt());
    }

    @Test
    void settledReceiptWithNoFinalPaymentHasNothingToPay() {
        Handover record = returnedBooking();
        record.setSettledAt(OffsetDateTime.now());
        when(bookingFeeService.getFeesByBooking(10L)).thenReturn(List.of());
        when(paymentService.getPaymentsByBooking(10L)).thenReturn(List.of());

        ReturnReceiptDTO receipt = handoverService.getReturnReceipt(10L);

        assertTrue(receipt.isNothingToPay());
        assertNull(receipt.getPaymentMethod());
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
        when(handoverRepository.findByIdAndBookingId(7L, 10L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> handoverService.loadEvidence(10L, 7L, false));
    }

    private Handover returnedBooking() {
        booking.setStatus("active");
        Handover record = new Handover();
        record.setType(HandoverType.CHECK_OUT);
        record.setOdometer(1300);
        record.setFuelPercent(60);
        record.setRecordedAt(OffsetDateTime.now());
        when(handoverRepository.findByBookingIdAndType(10L, HandoverType.CHECK_OUT)).thenReturn(Optional.of(record));
        when(handoverRepository.findByBookingIdOrderByRecordedAtAsc(10L)).thenReturn(List.of(record));
        return record;
    }

    private BookingFee fee(String amount) {
        BookingFee fee = new BookingFee();
        fee.setAmount(new BigDecimal(amount));
        return fee;
    }

    private Payment payment(String type, String status, String amount, String method) {
        Payment payment = new Payment();
        payment.setPaymentType(type);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(method);
        payment.setPaidAt(OffsetDateTime.now());
        return payment;
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

    private ReturnChargeDTO returnCharge(String type, String amount, String description) {
        ReturnChargeDTO charge = new ReturnChargeDTO();
        charge.setFeeType(type);
        charge.setAmount(amount != null ? new BigDecimal(amount) : null);
        charge.setDescription(description);
        return charge;
    }
}
