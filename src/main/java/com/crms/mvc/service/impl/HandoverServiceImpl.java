package com.crms.mvc.service.impl;

import com.crms.mvc.enums.PaymentMethod;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;
import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.HandoverRecordDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.dto.ReturnReceiptDTO;
import com.crms.mvc.dto.ReturnReceiptLineDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Handover;
import com.crms.mvc.enums.FeeType;
import com.crms.mvc.enums.HandoverType;
import com.crms.mvc.enums.IssueStatus;
import com.crms.mvc.enums.IssueType;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.HandoverRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.HandoverService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.util.EnumTokens;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class HandoverServiceImpl implements HandoverService {
    private static final String FEE_LATE = "LATE_FEE";
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "BANK_TRANSFER");

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final IssueRepository issueRepository;
    private final HandoverRepository handoverRepository;
    private final BookingFeeService bookingFeeService;
    private final ContractService contractService;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final FileUploadService fileUploadService;

    public HandoverServiceImpl(BookingRepository bookingRepository,
                               EmployeeRepository employeeRepository,
                               VehicleRepository vehicleRepository,
                               IssueRepository issueRepository,
                               HandoverRepository handoverRepository,
                               BookingFeeService bookingFeeService,
                               ContractService contractService,
                               PricingService pricingService,
                               PaymentService paymentService,
                               FileUploadService fileUploadService) {
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.vehicleRepository = vehicleRepository;
        this.issueRepository = issueRepository;
        this.handoverRepository = handoverRepository;
        this.bookingFeeService = bookingFeeService;
        this.contractService = contractService;
        this.pricingService = pricingService;
        this.paymentService = paymentService;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public void checkIn(Long bookingId, Long employeeId, CheckInDTO form) {
        Booking booking = findBooking(bookingId);
        Employee actor = requireHandler(booking, employeeId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Đơn thuê phải được xác nhận trước khi bàn giao xe");
        }
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Chưa thể bàn giao: đơn thuê chưa được gán xe");
        }
        validatePickupTimeAndVehicle(booking, vehicle);
        if (handoverRepository.existsByBookingIdAndType(bookingId, HandoverType.CHECK_IN)) {
            throw new IllegalStateException("Đơn thuê này đã có biên bản bàn giao xe");
        }
        if (isMissing(form.getLicenseImage()) || isMissing(form.getIdCardImage())) {
            throw new IllegalArgumentException("Cần chụp ảnh giấy phép lái xe và CCCD");
        }
        requireOdometer(vehicle, form.getOdometer());

        // One person drives one car at a time: the earlier rental must be settled before this one is handed over.
        List<Booking> stillOpen = bookingRepository.findOpenRentalsOfCustomerExcept(
                booking.getCustomer().getId(), bookingId);
        if (!stillOpen.isEmpty()) {
            throw new IllegalStateException("Khách còn đơn #" + stillOpen.get(0).getId()
                    + " chưa quyết toán. Chỉ bàn giao xe này sau khi khách quyết toán đơn đó.");
        }
        // The rest of the rent is paid here, so the car is only handed over once it is collected.
        BigDecimal rentalBalance = booking.getTotalPrice().subtract(booking.getDepositAmount());
        if (rentalBalance.signum() > 0
                && (form.getPaymentMethod() == null || !PAYMENT_METHODS.contains(form.getPaymentMethod()))) {
            throw new IllegalArgumentException("Vui lòng chọn tiền mặt hoặc chuyển khoản cho phần tiền thuê còn lại");
        }
        if (rentalBalance.signum() > 0 && !form.isPaymentReceived()) {
            throw new IllegalArgumentException("Vui lòng xác nhận đã nhận đủ tiền thuê còn lại");
        }

        Handover record = newHandover(booking, actor, HandoverType.CHECK_IN,
                form.getOdometer(), form.getFuelPercent(), form.getNotes());
        record.setLicenseImageUrl(fileUploadService.saveHandoverImage(form.getLicenseImage()));
        record.setIdCardImageUrl(fileUploadService.saveHandoverImage(form.getIdCardImage()));
        handoverRepository.save(record);
        if (rentalBalance.signum() > 0) {
            paymentService.recordRentalBalancePayment(bookingId, rentalBalance, PaymentMethod.from(form.getPaymentMethod()));
        }

        vehicle.setOdometer(form.getOdometer());
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicleRepository.save(vehicle);

        booking.setActualPickupTime(OffsetDateTime.now());
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);
    }

    @Override
    public void checkOut(Long bookingId, Long employeeId, CheckOutDTO form) {
        Booking booking = findBooking(bookingId);
        Employee actor = requireHandler(booking, employeeId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("Đơn thuê không ở trạng thái đang thuê");
        }
        if (handoverRepository.existsByBookingIdAndType(bookingId, HandoverType.CHECK_OUT)) {
            throw new IllegalStateException("Đơn thuê này đã được ghi nhận trả xe");
        }
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Đơn thuê chưa được gán xe");
        }
        requireOdometer(vehicle, form.getOdometer());

        OffsetDateTime returnedAt = OffsetDateTime.now();
        booking.setActualReturnTime(returnedAt);

        handoverRepository.save(newHandover(booking, actor, HandoverType.CHECK_OUT,
                form.getOdometer(), null, null));

        // The late fee belongs to the booking, not to any Issue. Damage, cleaning and the like are recorded as Issues.
        BigDecimal multiplier = contractService.getTermsOfBooking(bookingId).getLateFeeMultiplier();
        BigDecimal lateFee = pricingService.calculateLateFee(
                booking.getScheduledEndTime(), returnedAt, booking.getPricePerHour(), multiplier);
        if (lateFee != null && lateFee.signum() > 0) {
            bookingFeeService.recordFee(bookingId, FeeType.LATE_FEE, lateFee, "Phí trả xe trễ", null);
        }

        vehicle.setOdometer(form.getOdometer());
        vehicle.setStatus(form.isMoveToMaintenance() ? VehicleStatus.MAINTENANCE : VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
        bookingRepository.save(booking);
    }

    @Override
    public void settle(Long bookingId, Long employeeId, SettlementDTO form) {
        Booking booking = findBooking(bookingId);
        requireHandler(booking, employeeId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("Đơn thuê không ở bước chờ quyết toán");
        }
        Handover record = handoverRepository.findByBookingIdAndType(bookingId, HandoverType.CHECK_OUT)
                .orElseThrow(() -> new IllegalStateException("Cần ghi nhận trả xe trước khi quyết toán"));
        // ADR 0002: never settle while the customer may be paying the same Issue fees online.
        if (paymentService.hasLiveIssuePaymentSession(bookingId)) {
            throw new IllegalStateException("Khách đang có phiên thanh toán trực tuyến cho phí sự cố (hiệu lực 15 phút). "
                    + "Vui lòng đợi khách thanh toán xong hoặc phiên hết hạn rồi quyết toán lại.");
        }

        // The rent was paid in full by handover, so settlement only collects the fees still unpaid (late fee,
        // return charges, open Issues) and nothing is ever refunded here. Settlement closes the booking for good:
        // every Issue must be recorded before it.
        List<BookingFee> unpaid = bookingFeeService.getUnpaidFees(bookingId);
        BigDecimal finalAmount = sum(unpaid);
        if (finalAmount.signum() > 0) {
            if (form.getPaymentMethod() == null || !PAYMENT_METHODS.contains(form.getPaymentMethod())) {
                throw new IllegalArgumentException("Vui lòng chọn tiền mặt hoặc chuyển khoản");
            }
            Payment finalPayment = paymentService.recordFinalPayment(bookingId, finalAmount, PaymentMethod.from(form.getPaymentMethod()));
            // Ties every fee to the payment that collected it; open Issues among them become RESOLVED.
            if (!unpaid.isEmpty()) {
                bookingFeeService.attachPayment(unpaid, finalPayment);
            }
        }

        record.setSettledAt(OffsetDateTime.now());
        handoverRepository.save(record);

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    @Override
    public void reassignStaff(Long bookingId, Long staffId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Đơn thuê này đã đóng");
        }
        Employee staff = employeeRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên: " + staffId));
        if (!staff.isActive()) {
            throw new IllegalArgumentException("Tài khoản nhân viên này đang bị khoá");
        }
        booking.setEmployee(staff);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAssignableEmployees() {
        return employeeRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverSummaryDTO getSummary(Long bookingId) {
        Booking booking = findBooking(bookingId);
        HandoverSummaryDTO summary = new HandoverSummaryDTO();
        List<HandoverRecordDTO> records = new ArrayList<>();
        for (Handover handover : handoverRepository.findByBookingIdOrderByRecordedAtAsc(bookingId)) {
            records.add(toRecord(handover));
            if (handover.getType() == HandoverType.CHECK_OUT) {
                summary.setReturned(true);
                summary.setSettled(handover.isSettled());
            }
        }

        List<BookingFee> unpaid = bookingFeeService.getUnpaidFees(bookingId);
        summary.setRecords(records);
        summary.setFeesTotal(sum(unpaid));
        summary.setPrepaidTotal(bookingFeeService.calculatePrepaidIssueFees(bookingId));
        summary.setFinalAmount(sum(unpaid));
        summary.setVehicleDamageReported(issueRepository.existsByBookingIdAndIssueTypeInAndStatusNot(
                bookingId, List.of(IssueType.COLLISION, IssueType.DAMAGE), IssueStatus.CANCELLED));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnReceiptDTO getReturnReceipt(Long bookingId) {
        Optional<Handover> returnRecord = handoverRepository.findByBookingIdAndType(bookingId, HandoverType.CHECK_OUT);
        if (returnRecord.isEmpty()) {
            return null;
        }
        Booking booking = findBooking(bookingId);
        Handover record = returnRecord.get();

        ReturnReceiptDTO receipt = new ReturnReceiptDTO();
        OffsetDateTime returnedAt = booking.getActualReturnTime() != null
                ? booking.getActualReturnTime() : record.getRecordedAt();
        receipt.setReturnedAt(DateTimeUtils.toVietnamTime(returnedAt));
        receipt.setOdometer(record.getOdometer());
        receipt.setRentalTotal(booking.getTotalPrice());
        receipt.setDepositAmount(booking.getDepositAmount());
        for (BookingFee fee : bookingFeeService.getFeesByBooking(bookingId)) {
            String description = hasText(fee.getDescription()) ? fee.getDescription() : (fee.getFeeType() != null ? fee.getFeeType().getLabel() : "");
            receipt.getLines().add(new ReturnReceiptLineDTO(description, fee.getAmount(), isPaidEarlier(fee)));
        }

        receipt.setSettled(record.isSettled());
        if (record.isSettled()) {
            Optional<Payment> finalPayment = paymentService.getPaymentsByBooking(bookingId).stream()
                    .filter(p -> PaymentType.FINAL == p.getPaymentType()
                            && PaymentStatus.PAID == p.getStatus())
                    .findFirst();
            receipt.setAmountDue(finalPayment.map(Payment::getAmount).orElse(BigDecimal.ZERO));
            finalPayment.ifPresent(p -> {
                receipt.setPaymentMethod(EnumTokens.name(p.getPaymentMethod()));
                receipt.setPaidAt(DateTimeUtils.toVietnamTime(p.getPaidAt()));
            });
        } else {
            receipt.setAmountDue(sum(bookingFeeService.getUnpaidFees(bookingId)));
        }
        return receipt;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadEvidence(Long bookingId, Long handoverId, boolean secondary) {
        Handover handover = handoverRepository.findByIdAndBookingId(handoverId, bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy biên bản: " + handoverId));
        String fileName = secondary ? handover.getIdCardImageUrl() : handover.getLicenseImageUrl();
        return fileName == null ? null : fileUploadService.loadHandoverImage(fileName);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
    }

    private Employee requireHandler(Booking booking, Long employeeId) {
        Employee actor = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên: " + employeeId));
        boolean assigned = booking.getEmployee() != null && employeeId.equals(booking.getEmployee().getId());
        if (!assigned && actor.getRole() != EmployeeRole.ADMIN) {
            throw new IllegalStateException("Chỉ nhân viên phụ trách hoặc quản trị viên được thực hiện thao tác này");
        }
        return actor;
    }

    private void requireOdometer(Vehicle vehicle, Integer odometer) {
        if (odometer == null || odometer < vehicle.getOdometer()) {
            throw new IllegalArgumentException("Số km không được nhỏ hơn số km hiện tại của xe là "
                    + vehicle.getOdometer() + " km");
        }
    }

    private boolean isPaidEarlier(BookingFee fee) {
        Payment payment = fee.getPayment();
        return payment != null
                && PaymentStatus.PAID == payment.getStatus()
                && PaymentType.FINAL != payment.getPaymentType();
    }

    private BigDecimal sum(List<BookingFee> fees) {
        return fees.stream().map(BookingFee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Handover newHandover(Booking booking, Employee actor, HandoverType type,
                                 Integer odometer, Integer fuelPercent, String notes) {
        Handover handover = new Handover();
        handover.setBooking(booking);
        handover.setEmployee(actor);
        handover.setType(type);
        handover.setOdometer(odometer);
        handover.setFuelPercent(fuelPercent);
        handover.setNotes(hasText(notes) ? notes.trim() : null);
        handover.setRecordedAt(OffsetDateTime.now());
        return handover;
    }

    private HandoverRecordDTO toRecord(Handover handover) {
        HandoverRecordDTO record = new HandoverRecordDTO();
        record.setId(handover.getId());
        record.setType(handover.getType().name());
        record.setTypeLabel(handover.getType().getLabel());
        record.setOdometer(handover.getOdometer());
        record.setFuelPercent(handover.getFuelPercent());
        record.setNotes(handover.getNotes());
        record.setEmployeeName(handover.getEmployee() != null ? handover.getEmployee().getName() : null);
        record.setRecordedAt(DateTimeUtils.toVietnamTime(handover.getRecordedAt()));
        record.setLicenseImage(handover.getLicenseImageUrl() != null);
        record.setIdCardImage(handover.getIdCardImageUrl() != null);
        return record;
    }

    /**
     * The car is handed over from the scheduled pickup time, never earlier. Whether a returned car is clean
     * is decided by staff in person; if the assigned car is not available, staff reassign another car first.
     */
    private void validatePickupTimeAndVehicle(Booking booking, Vehicle vehicle) {
        OffsetDateTime scheduledPickup = booking.getPickupTime();
        if (scheduledPickup != null && OffsetDateTime.now().isBefore(scheduledPickup)) {
            throw new IllegalStateException("Chưa đến giờ nhận xe. Chỉ được bàn giao từ "
                    + DateTimeUtils.formatToVietnamTime(scheduledPickup) + ".");
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Xe được gán chưa sẵn sàng bàn giao (trạng thái: " + vehicle.getStatus()
                    + "). Hãy đổi sang xe khác cùng loại cho đơn này.");
        }
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}