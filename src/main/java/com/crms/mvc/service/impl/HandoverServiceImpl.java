package com.crms.mvc.service.impl;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.HandoverRecordDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.dto.ReturnChargeDTO;
import com.crms.mvc.dto.ReturnReceiptDTO;
import com.crms.mvc.dto.ReturnReceiptLineDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Handover;
import com.crms.mvc.entity.HandoverType;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
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
import com.crms.mvc.util.EmployeeRole;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class HandoverServiceImpl implements HandoverService {
    private static final String FEE_LATE = "LATE_FEE";
    /** Return charge types and their Vietnamese labels. Damage is never a Return charge: it must be recorded as an Issue. */
    private static final Map<String, String> RETURN_CHARGE_LABELS = Map.of(
            "CLEANING_FEE", "Phí vệ sinh xe",
            "TOLL_FEE", "Phí cầu đường",
            "OTHER_FEE", "Phí khác");
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

        if (!"confirmed".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn thuê phải được xác nhận trước khi bàn giao xe");
        }
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Chưa thể bàn giao: đơn thuê chưa được gán xe");
        }
        validateEarlyPickupAndVehicleAvailability(booking);
        if (handoverRepository.existsByBookingIdAndType(bookingId, HandoverType.CHECK_IN)) {
            throw new IllegalStateException("Đơn thuê này đã có biên bản bàn giao xe");
        }
        if (isMissing(form.getLicenseImage()) || isMissing(form.getIdCardImage())) {
            throw new IllegalArgumentException("Cần chụp ảnh giấy phép lái xe và CCCD");
        }
        requireOdometer(vehicle, form.getOdometer());

        Handover record = newHandover(booking, actor, HandoverType.CHECK_IN,
                form.getOdometer(), form.getFuelPercent(), form.getNotes());
        record.setLicenseImageUrl(fileUploadService.saveHandoverImage(form.getLicenseImage()));
        record.setIdCardImageUrl(fileUploadService.saveHandoverImage(form.getIdCardImage()));
        handoverRepository.save(record);

        vehicle.setOdometer(form.getOdometer());
        vehicle.setStatus("rented");
        vehicleRepository.save(vehicle);

        booking.setActualPickupTime(OffsetDateTime.now());
        booking.setStatus("active");
        bookingRepository.save(booking);
    }

    @Override
    public void checkOut(Long bookingId, Long employeeId, CheckOutDTO form) {
        Booking booking = findBooking(bookingId);
        Employee actor = requireHandler(booking, employeeId);

        if (!"active".equalsIgnoreCase(booking.getStatus())) {
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
        List<ReturnChargeDTO> returnCharges = filledReturnCharges(form);

        OffsetDateTime returnedAt = OffsetDateTime.now();
        booking.setActualReturnTime(returnedAt);

        handoverRepository.save(newHandover(booking, actor, HandoverType.CHECK_OUT,
                form.getOdometer(), form.getFuelPercent(), form.getNotes()));

        // Late fee and Return charges belong to the booking, not to any Issue.
        BigDecimal multiplier = contractService.getTermsOfBooking(bookingId).getLateFeeMultiplier();
        BigDecimal lateFee = pricingService.calculateLateFee(
                booking.getScheduledEndTime(), returnedAt, booking.getPricePerHour(), multiplier);
        if (lateFee != null && lateFee.signum() > 0) {
            bookingFeeService.recordFee(bookingId, FEE_LATE, lateFee, "Phí trả xe trễ", null);
        }
        for (ReturnChargeDTO charge : returnCharges) {
            String description = hasText(charge.getDescription())
                    ? charge.getDescription().trim()
                    : RETURN_CHARGE_LABELS.get(charge.getFeeType());
            bookingFeeService.recordFee(bookingId, charge.getFeeType(), charge.getAmount(), description, null);
        }

        vehicle.setOdometer(form.getOdometer());
        vehicle.setStatus(form.isMoveToMaintenance() ? "maintenance" : "available");
        vehicleRepository.save(vehicle);
        bookingRepository.save(booking);
    }

    @Override
    public void settle(Long bookingId, Long employeeId, SettlementDTO form) {
        Booking booking = findBooking(bookingId);
        requireHandler(booking, employeeId);

        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Đơn thuê không ở bước chờ quyết toán");
        }
        Handover record = handoverRepository.findByBookingIdAndType(bookingId, HandoverType.CHECK_OUT)
                .orElseThrow(() -> new IllegalStateException("Cần ghi nhận trả xe trước khi quyết toán"));
        // ADR 0002: never settle while the customer may be paying the same Issue fees online.
        if (paymentService.hasLiveIssuePaymentSession(bookingId)) {
            throw new IllegalStateException("Khách đang có phiên thanh toán trực tuyến cho phí sự cố (hiệu lực 15 phút). "
                    + "Vui lòng đợi khách thanh toán xong hoặc phiên hết hạn rồi quyết toán lại.");
        }

        // The deposit is a share (at most 100%) of the rent, so it never covers fees and nothing is ever refunded
        // here: the customer pays the rest of the rent plus every unpaid fee. Zero only when the deposit was the
        // full rent and there are no fees.
        List<BookingFee> unpaid = bookingFeeService.getUnpaidFees(bookingId);
        BigDecimal finalAmount = finalAmount(booking, unpaid);
        if (finalAmount.signum() < 0) {
            throw new IllegalStateException("The deposit is larger than the rental total and fees. "
                    + "Check the booking amounts before settling.");
        }
        if (finalAmount.signum() > 0) {
            if (form.getPaymentMethod() == null || !PAYMENT_METHODS.contains(form.getPaymentMethod())) {
                throw new IllegalArgumentException("Vui lòng chọn tiền mặt hoặc chuyển khoản");
            }
            Payment finalPayment = paymentService.recordFinalPayment(bookingId, finalAmount, form.getPaymentMethod());
            // Ties every fee to the payment that collected it; open Issues among them become RESOLVED.
            if (!unpaid.isEmpty()) {
                bookingFeeService.attachPayment(unpaid, finalPayment);
            }
        }

        record.setSettledAt(OffsetDateTime.now());
        handoverRepository.save(record);

        booking.setStatus("completed");
        bookingRepository.save(booking);
    }

    @Override
    public void reassignStaff(Long bookingId, Long staffId) {
        Booking booking = findBooking(bookingId);
        if ("completed".equalsIgnoreCase(booking.getStatus()) || "cancelled".equalsIgnoreCase(booking.getStatus())) {
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
        summary.setRentalTotal(booking.getTotalPrice());
        summary.setFeesTotal(sum(unpaid));
        summary.setPrepaidTotal(bookingFeeService.calculatePrepaidIssueFees(bookingId));
        summary.setDepositAmount(booking.getDepositAmount());
        summary.setFinalAmount(finalAmount(booking, unpaid));
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
        receipt.setFuelPercent(record.getFuelPercent());
        receipt.setNotes(record.getNotes());
        receipt.setRentalTotal(booking.getTotalPrice());
        receipt.setDepositAmount(booking.getDepositAmount());
        for (BookingFee fee : bookingFeeService.getFeesByBooking(bookingId)) {
            String description = hasText(fee.getDescription()) ? fee.getDescription() : fee.getFeeType();
            receipt.getLines().add(new ReturnReceiptLineDTO(description, fee.getAmount(), isPaidEarlier(fee)));
        }

        receipt.setSettled(record.isSettled());
        if (record.isSettled()) {
            Optional<Payment> finalPayment = paymentService.getPaymentsByBooking(bookingId).stream()
                    .filter(p -> Payment.TYPE_FINAL.equals(p.getPaymentType())
                            && Payment.STATUS_PAID.equalsIgnoreCase(p.getStatus()))
                    .findFirst();
            receipt.setAmountDue(finalPayment.map(Payment::getAmount).orElse(BigDecimal.ZERO));
            finalPayment.ifPresent(p -> {
                receipt.setPaymentMethod(p.getPaymentMethod());
                receipt.setPaidAt(DateTimeUtils.toVietnamTime(p.getPaidAt()));
            });
        } else {
            receipt.setAmountDue(finalAmount(booking, bookingFeeService.getUnpaidFees(bookingId)).max(BigDecimal.ZERO));
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
        if (!assigned && !EmployeeRole.ADMIN.equals(actor.getRole())) {
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

    private List<ReturnChargeDTO> filledReturnCharges(CheckOutDTO form) {
        List<ReturnChargeDTO> filled = new ArrayList<>();
        for (ReturnChargeDTO charge : form.getReturnCharges()) {
            if (charge == null || charge.getAmount() == null || charge.getAmount().signum() == 0) {
                continue;
            }
            if (charge.getAmount().signum() < 0) {
                throw new IllegalArgumentException("Phí trả xe không được âm");
            }
            if (charge.getFeeType() == null || !RETURN_CHARGE_LABELS.containsKey(charge.getFeeType())) {
                throw new IllegalArgumentException("Loại phí trả xe không hợp lệ");
            }
            filled.add(charge);
        }
        return filled;
    }

    private BigDecimal finalAmount(Booking booking, List<BookingFee> unpaid) {
        return booking.getTotalPrice()
                .add(sum(unpaid))
                .subtract(booking.getDepositAmount());
    }

    private boolean isPaidEarlier(BookingFee fee) {
        Payment payment = fee.getPayment();
        return payment != null
                && Payment.STATUS_PAID.equalsIgnoreCase(payment.getStatus())
                && !Payment.TYPE_FINAL.equals(payment.getPaymentType());
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

    public static final int MAX_EARLY_PICKUP_MINUTES = 30;
    public static final int CLEANING_WINDOW_MINUTES = 30;

    private void validateEarlyPickupAndVehicleAvailability(Booking booking) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime scheduledPickup = booking.getPickupTime();
        Vehicle vehicle = booking.getVehicle();

        // 1. Chỉ được nhận xe trước giờ hẹn tối đa 30 phút
        if (scheduledPickup != null && now.isBefore(scheduledPickup.minusMinutes(MAX_EARLY_PICKUP_MINUTES))) {
            throw new IllegalStateException(
                    "Chỉ được phép nhận xe trước giờ hẹn tối đa 30 phút. Giờ nhận xe dự kiến là: "
                            + DateTimeUtils.formatToVietnamTime(scheduledPickup) + ".");
        }

        // 2. Xe phải ở trạng thái sẵn sàng (available)
        if (vehicle != null && !Vehicle.STATUS_AVAILABLE.equalsIgnoreCase(vehicle.getStatus())) {
            if (scheduledPickup != null && now.isBefore(scheduledPickup)) {
                throw new IllegalStateException(
                        "Phương tiện hiện đang trong quy trình kiểm tra và vệ sinh sau ca thuê trước. "
                                + "Quý khách vui lòng nhận xe vào đúng khung giờ dự kiến ("
                                + DateTimeUtils.formatToVietnamTime(scheduledPickup)
                                + "), hoặc đổi sang phương tiện cùng loại khác biển số nếu muốn nhận ngay.");
            } else {
                throw new IllegalStateException(
                        "Phương tiện chưa sẵn sàng bàn giao (trạng thái hiện tại: " + vehicle.getStatus()
                                + "). Vui lòng kiểm tra lại tình trạng xe hoặc đổi sang xe khác cùng loại.");
            }
        }

        // 3. Cơ chế suy diễn thời gian (Computed State): Xe vừa trả của ca trước chưa đủ 30 phút dọn rửa
        if (vehicle != null && vehicle.getId() != null) {
            List<Booking> recentReturns = bookingRepository.findRecentReturnsByVehicle(vehicle.getId(), booking.getId());
            if (!recentReturns.isEmpty()) {
                Booking latestReturn = recentReturns.get(0);
                if (latestReturn.getActualReturnTime() != null) {
                    OffsetDateTime cleaningDoneAt = latestReturn.getActualReturnTime().plusMinutes(CLEANING_WINDOW_MINUTES);
                    if (now.isBefore(cleaningDoneAt)) {
                        throw new IllegalStateException(
                                "Phương tiện hiện đang trong quy trình kiểm tra và vệ sinh sau ca thuê trước (dự kiến hoàn tất lúc "
                                        + DateTimeUtils.formatToVietnamTime(cleaningDoneAt) + "). "
                                        + (scheduledPickup != null ? "Quý khách vui lòng nhận xe vào đúng khung giờ dự kiến ("
                                        + DateTimeUtils.formatToVietnamTime(scheduledPickup) + ")" : "Quý khách vui lòng đợi trong giây lát")
                                        + ", hoặc liên hệ nhân viên để được hỗ trợ đổi sang phương tiện cùng loại khác biển số nếu muốn nhận ngay.");
                    }
                }
            }
        }
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}