package com.crms.mvc.service;

import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.ReturnReceiptDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Handover;
import com.crms.mvc.enums.FeeType;
import com.crms.mvc.enums.HandoverType;
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
        assigned = employee(2L, EmployeeRole.STAFF);
        Employee other = employee(3L, EmployeeRole.STAFF);
        Employee admin = employee(1L, EmployeeRole.ADMIN);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(assigned));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(other));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(admin));

        vehicle = new Vehicle();
        vehicle.setOdometer(1000);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        Customer customer = new Customer();
        customer.setId(5L);

        booking = new Booking();
        booking.setId(10L);
        booking.setCustomer(customer);
        booking.setEmployee(assigned);
        booking.setVehicle(vehicle);
        booking.setStatus(BookingStatus.CONFIRMED);
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
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
        assertEquals(1200, vehicle.getOdometer());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void handoverCollectsTheRestOfTheRentAndRecordsTheMethod() {
        handoverService.checkIn(10L, 2L, checkInForm(1200));

        // total 1,000,000 minus deposit 300,000
        verify(paymentService).recordRentalBalancePayment(10L, new BigDecimal("700000"), PaymentMethod.CASH);
    }

    @Test
    void theCarIsNotHandedOverUntilHowTheRestOfTheRentWasPaidIsChosen() {
        CheckInDTO form = checkInForm(1200);
        form.setPaymentMethod(null);

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, form));

        verify(handoverRepository, never()).save(any());
        verify(paymentService, never()).recordRentalBalancePayment(any(), any(), any());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void theCarIsNotHandedOverUntilStaffConfirmTheyReceivedTheMoney() {
        CheckInDTO form = checkInForm(1200);
        form.setPaymentReceived(false);

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, form));

        verify(handoverRepository, never()).save(any());
        verify(paymentService, never()).recordRentalBalancePayment(any(), any(), any());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void anUnknownPaymentMethodIsRefused() {
        CheckInDTO form = checkInForm(1200);
        form.setPaymentMethod("VNPAY");

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, form));
    }

    @Test
    void whenTheDepositAlreadyCoversTheRentNothingIsCollectedAtHandover() {
        booking.setDepositAmount(new BigDecimal("1000000"));
        CheckInDTO form = checkInForm(1200);
        form.setPaymentMethod(null);

        handoverService.checkIn(10L, 2L, form);

        verify(paymentService, never()).recordRentalBalancePayment(any(), any(), any());
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }

    @Test
    void theCarIsNotHandedOverWhileTheCustomerHasAnEarlierRentalUnsettled() {
        Booking earlier = new Booking();
        earlier.setId(9L);
        when(bookingRepository.findOpenRentalsOfCustomerExcept(5L, 10L)).thenReturn(List.of(earlier));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> handoverService.checkIn(10L, 2L, checkInForm(1200)));

        assertTrue(error.getMessage().contains("#9"));
        verify(handoverRepository, never()).save(any());
        verify(paymentService, never()).recordRentalBalancePayment(any(), any(), any());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void adminCheckInKeepsAssignedStaff() {
        handoverService.checkIn(10L, 1L, checkInForm(1200));

        assertEquals(assigned, booking.getEmployee());
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }

    @Test
    void otherStaffCannotCheckIn() {
        assertThrows(IllegalStateException.class, () -> handoverService.checkIn(10L, 3L, checkInForm(1200)));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(handoverRepository, never()).save(any());
    }

    @Test
    void lowerOdometerIsRejectedAtCheckIn() {
        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, checkInForm(999)));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void missingPhotoIsRejectedAtCheckIn() {
        CheckInDTO form = checkInForm(1200);
        form.setIdCardImage(new MockMultipartFile("idCardImage", new byte[0]));

        assertThrows(IllegalArgumentException.class, () -> handoverService.checkIn(10L, 2L, form));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void secondCheckInIsRejected() {
        when(handoverRepository.existsByBookingIdAndType(10L, HandoverType.CHECK_IN)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> handoverService.checkIn(10L, 2L, checkInForm(1200)));
    }

    @Test
    void checkOutRecordsTheLateFeeAgainstTheBookingOnly() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(new BigDecimal("150000"));
        CheckOutDTO form = checkOutForm(1300);

        handoverService.checkOut(10L, 2L, form);

        verify(bookingFeeService).recordFee(10L, FeeType.LATE_FEE, new BigDecimal("150000"), "Phí trả xe trễ", null);
        // only the late fee: damage, cleaning and the like are recorded as Issues, never at the return form
        verify(bookingFeeService, org.mockito.Mockito.times(1)).recordFee(any(), any(FeeType.class), any(), any(), any());
        ArgumentCaptor<Handover> saved = ArgumentCaptor.forClass(Handover.class);
        verify(handoverRepository).save(saved.capture());
        assertEquals(HandoverType.CHECK_OUT, saved.getValue().getType());
        assertNull(saved.getValue().getFuelPercent());
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        assertEquals(1300, vehicle.getOdometer());
        assertNotNull(booking.getActualReturnTime());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void maintenanceIsWhateverStaffTicked() {
        booking.setStatus(BookingStatus.ACTIVE);
        CheckOutDTO form = checkOutForm(1300);
        form.setMoveToMaintenance(true);

        handoverService.checkOut(10L, 2L, form);

        assertEquals(VehicleStatus.MAINTENANCE, vehicle.getStatus());
    }

    @Test
    void otherStaffCannotCheckOutOrSettle() {
        booking.setStatus(BookingStatus.ACTIVE);

        assertThrows(IllegalStateException.class, () -> handoverService.checkOut(10L, 3L, checkOutForm(1300)));
        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 3L, new SettlementDTO()));
    }

    @Test
    void secondCheckOutIsRejected() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(handoverRepository.existsByBookingIdAndType(10L, HandoverType.CHECK_OUT)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> handoverService.checkOut(10L, 2L, checkOutForm(1300)));
    }

    @Test
    void settlementCollectsOnlyUnpaidFeesAndTiesThemToTheFinalPayment() {
        Handover record = returnedBooking();
        List<BookingFee> unpaid = List.of(fee("100000"));
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(unpaid);
        Payment finalPayment = new Payment();
        when(paymentService.recordFinalPayment(10L, new BigDecimal("100000"), PaymentMethod.CASH)).thenReturn(finalPayment);
        SettlementDTO form = new SettlementDTO();
        form.setPaymentMethod("CASH");

        handoverService.settle(10L, 2L, form);

        verify(bookingFeeService).attachPayment(unpaid, finalPayment);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
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
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }

    @Test
    void settlementRequiresPaymentMethodWhenCustomerOwes() {
        returnedBooking();
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of(fee("100000")));

        assertThrows(IllegalArgumentException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }

    @Test
    void noFeesClosesTheBookingWithoutAnyPayment() {
        returnedBooking();

        handoverService.settle(10L, 2L, new SettlementDTO());

        verify(paymentService, never()).recordFinalPayment(any(), any(), any());
        verify(paymentService, never()).createPendingRefund(any(), any(), any(), any(), any());
        verify(bookingFeeService, never()).attachPayment(anyList(), any());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    @Test
    void settlementBeforeReturnOrAfterCompletionIsRejected() {
        booking.setStatus(BookingStatus.ACTIVE);
        assertThrows(IllegalStateException.class, () -> handoverService.settle(10L, 2L, new SettlementDTO()));

        booking.setStatus(BookingStatus.COMPLETED);
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
        assertEquals(new BigDecimal("100000"), summary.getFinalAmount());
        assertTrue(summary.isReturned());
        assertFalse(summary.isSettled());
        assertTrue(summary.isVehicleDamageReported());
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
        prepaid.setPayment(payment(PaymentType.ISSUE_COMPENSATION, PaymentStatus.PAID, "500000", PaymentMethod.VNPAY));
        when(bookingFeeService.getFeesByBooking(10L)).thenReturn(List.of(late, prepaid));
        when(bookingFeeService.getUnpaidFees(10L)).thenReturn(List.of(late));

        ReturnReceiptDTO receipt = handoverService.getReturnReceipt(10L);

        assertFalse(receipt.isSettled());
        assertEquals(1300, receipt.getOdometer());
        assertEquals(2, receipt.getLines().size());
        assertFalse(receipt.getLines().get(0).paidEarlier());
        assertTrue(receipt.getLines().get(1).paidEarlier());
        assertEquals(new BigDecimal("150000"), receipt.getAmountDue());
    }

    @Test
    void returnReceiptAfterSettlementShowsTheFinalPayment() {
        Handover record = returnedBooking();
        record.setSettledAt(OffsetDateTime.now());
        BookingFee late = fee("150000");
        Payment finalPayment = payment(PaymentType.FINAL, PaymentStatus.PAID, "850000", PaymentMethod.CASH);
        late.setPayment(finalPayment);
        when(bookingFeeService.getFeesByBooking(10L)).thenReturn(List.of(late));
        when(paymentService.getPaymentsByBooking(10L)).thenReturn(List.of(
                payment(PaymentType.DEPOSIT, PaymentStatus.PAID, "300000", PaymentMethod.VNPAY), finalPayment));

        ReturnReceiptDTO receipt = handoverService.getReturnReceipt(10L);

        assertTrue(receipt.isSettled());
        assertFalse(receipt.getLines().get(0).paidEarlier());
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
        Employee locked = employee(4L, EmployeeRole.STAFF);
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
        booking.setStatus(BookingStatus.ACTIVE);
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

    private Payment payment(PaymentType type, PaymentStatus status, String amount, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setPaymentType(type);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(method);
        payment.setPaidAt(OffsetDateTime.now());
        return payment;
    }

    private Employee employee(Long id, EmployeeRole role) {
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
        form.setPaymentMethod("CASH");
        form.setPaymentReceived(true);
        form.setDocumentsVerified(true);
        form.setLicenseImage(new MockMultipartFile("licenseImage", "l.jpg", "image/jpeg", new byte[]{1}));
        form.setIdCardImage(new MockMultipartFile("idCardImage", "i.jpg", "image/jpeg", new byte[]{1}));
        return form;
    }

    private CheckOutDTO checkOutForm(int odometer) {
        CheckOutDTO form = new CheckOutDTO();
        form.setOdometer(odometer);
        return form;
    }

}
