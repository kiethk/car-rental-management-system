package com.crms.mvc.service.impl;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.HandoverRecordDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.dto.SurchargeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.HandoverService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
public class HandoverServiceImpl implements HandoverService {
    private static final String TYPE_CHECK_IN = "CHECK_IN";
    private static final String TYPE_CHECK_OUT = "CHECK_OUT";
    private static final String TYPE_DAMAGE = "DAMAGE";
    private static final String STATUS_OPEN = "open";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String FEE_DAMAGE = "DAMAGE_COMPENSATION";
    private static final String FEE_LATE = "LATE_FEE";
    private static final Set<String> SURCHARGE_TYPES =
            Set.of(FEE_DAMAGE, "CLEANING_FEE", "FUEL_FEE", "TOLL_FEE", "OTHER_FEE");
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "BANK_TRANSFER");

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final IssueRepository issueRepository;
    private final BookingFeeService bookingFeeService;
    private final ContractService contractService;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final FileUploadService fileUploadService;

    public HandoverServiceImpl(BookingRepository bookingRepository,
                               EmployeeRepository employeeRepository,
                               VehicleRepository vehicleRepository,
                               IssueRepository issueRepository,
                               BookingFeeService bookingFeeService,
                               ContractService contractService,
                               PricingService pricingService,
                               PaymentService paymentService,
                               FileUploadService fileUploadService) {
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.vehicleRepository = vehicleRepository;
        this.issueRepository = issueRepository;
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
            throw new IllegalStateException("Booking must be confirmed before check-in");
        }
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Cannot check-in: vehicle is not assigned yet");
        }
        validateEarlyPickupAndVehicleAvailability(booking);
        if (issueRepository.findByBookingIdAndIssueType(bookingId, TYPE_CHECK_IN).isPresent()) {
            throw new IllegalStateException("Handover record already exists for this booking");
        }
        if (isMissing(form.getLicenseImage()) || isMissing(form.getIdCardImage())) {
            throw new IllegalArgumentException("Driving license and ID card photos are required");
        }
        requireOdometer(vehicle, form.getOdometer());

        Issue record = newIssue(booking, actor, TYPE_CHECK_IN, STATUS_RESOLVED,
                subject("Handover", form.getOdometer(), form.getFuelPercent(), form.getNotes()));
        record.setEvidenceUrl(fileUploadService.saveHandoverImage(form.getLicenseImage()));
        record.setSecondaryEvidenceUrl(fileUploadService.saveHandoverImage(form.getIdCardImage()));
        issueRepository.save(record);

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
            throw new IllegalStateException("Booking is not currently active");
        }
        if (issueRepository.findByBookingIdAndIssueType(bookingId, TYPE_CHECK_OUT).isPresent()) {
            throw new IllegalStateException("Return has already been recorded for this booking");
        }
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Booking has no vehicle assigned");
        }
        requireOdometer(vehicle, form.getOdometer());
        List<SurchargeDTO> surcharges = filledSurcharges(form);

        OffsetDateTime returnedAt = OffsetDateTime.now();
        booking.setActualReturnTime(returnedAt);

        Issue record = issueRepository.save(newIssue(booking, actor, TYPE_CHECK_OUT, STATUS_OPEN,
                subject("Return", form.getOdometer(), form.getFuelPercent(), form.getNotes())));

        BigDecimal multiplier = contractService.getTermsOfBooking(bookingId).getLateFeeMultiplier();
        BigDecimal lateFee = pricingService.calculateLateFee(
                booking.getScheduledEndTime(), returnedAt, booking.getPricePerHour(), multiplier);
        if (lateFee != null && lateFee.signum() > 0) {
            bookingFeeService.recordFee(bookingId, FEE_LATE, lateFee, "Late return fee", record.getId());
        }

        String damageEvidence = null;
        for (SurchargeDTO surcharge : surcharges) {
            String description = hasText(surcharge.getDescription())
                    ? surcharge.getDescription().trim()
                    : surcharge.getFeeType();
            Long issueId = record.getId();
            if (FEE_DAMAGE.equals(surcharge.getFeeType())) {
                if (damageEvidence == null && !isMissing(form.getDamageImage())) {
                    damageEvidence = fileUploadService.saveHandoverImage(form.getDamageImage());
                }
                Issue damage = newIssue(booking, actor, TYPE_DAMAGE, STATUS_OPEN, description);
                damage.setEvidenceUrl(damageEvidence);
                issueId = issueRepository.save(damage).getId();
            }
            bookingFeeService.recordFee(bookingId, surcharge.getFeeType(), surcharge.getAmount(), description, issueId);
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
            throw new IllegalStateException("Booking is not awaiting settlement");
        }
        Issue record = issueRepository.findByBookingIdAndIssueType(bookingId, TYPE_CHECK_OUT)
                .orElseThrow(() -> new IllegalStateException("Record the vehicle return before settling"));

        BigDecimal finalAmount = finalAmount(booking);
        if (finalAmount.signum() > 0) {
            if (form.getPaymentMethod() == null || !PAYMENT_METHODS.contains(form.getPaymentMethod())) {
                throw new IllegalArgumentException("Select cash or bank transfer");
            }
            paymentService.recordFinalPayment(bookingId, finalAmount, form.getPaymentMethod());
        } else if (finalAmount.signum() < 0) {
            if (!hasText(form.getBankCode()) || !hasText(form.getAccountNumber()) || !hasText(form.getAccountHolder())) {
                throw new IllegalArgumentException("Bank code, account number and account holder are required for a refund");
            }
            paymentService.createPendingRefund(bookingId, finalAmount.abs(), form.getBankCode(),
                    form.getAccountNumber(), form.getAccountHolder());
        }

        record.setStatus(STATUS_RESOLVED);
        issueRepository.save(record);

        for (Issue issue : issueRepository.findByBookingIdOrderByReportedAtAsc(bookingId)) {
            if (issue.getStatus() == IssueStatus.OPEN) {
                issue.setStatus(IssueStatus.RESOLVED);
                issueRepository.save(issue);
            }
        }

        booking.setStatus("completed");
        bookingRepository.save(booking);
    }

    @Override
    public void reassignStaff(Long bookingId, Long staffId) {
        Booking booking = findBooking(bookingId);
        if ("completed".equalsIgnoreCase(booking.getStatus()) || "cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("This booking is already closed");
        }
        Employee staff = employeeRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + staffId));
        if (!staff.isActive()) {
            throw new IllegalArgumentException("This employee account is locked");
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
        List<HandoverRecordDTO> records = new ArrayList<>();
        HandoverSummaryDTO summary = new HandoverSummaryDTO();

        for (Issue issue : issueRepository.findByBookingIdOrderByReportedAtAsc(bookingId)) {
            HandoverRecordDTO record = new HandoverRecordDTO();
            record.setId(issue.getId());
            record.setType(issue.getIssueType() != null ? issue.getIssueType().name() : null);
            record.setSubject(issue.getSubject());
            record.setStatus(issue.getStatus() != null ? issue.getStatus().dbValue() : null);
            record.setEmployeeName(issue.getEmployee() != null ? issue.getEmployee().getName() : null);
            record.setReportedAt(issue.getReportedAt());
            record.setEvidence(issue.getEvidenceUrl() != null);
            record.setSecondaryEvidence(issue.getSecondaryEvidenceUrl() != null);
            records.add(record);

            if (issue.getIssueType() != null && TYPE_CHECK_OUT.equalsIgnoreCase(issue.getIssueType().name())) {
                summary.setReturned(true);
                summary.setSettled(issue.getStatus() == IssueStatus.RESOLVED || STATUS_RESOLVED.equalsIgnoreCase(issue.getStatus() != null ? issue.getStatus().dbValue() : null));
            }
        }

        summary.setRecords(records);
        summary.setRentalTotal(booking.getTotalPrice());
        summary.setFeesTotal(bookingFeeService.calculateTotalAdditionalFees(bookingId));
        summary.setDepositAmount(booking.getDepositAmount());
        summary.setFinalAmount(finalAmount(booking));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadEvidence(Long bookingId, Long issueId, boolean secondary) {
        Issue issue = issueRepository.findById(issueId)
                .filter(found -> found.getBooking() != null && bookingId.equals(found.getBooking().getId()))
                .orElseThrow(() -> new NoSuchElementException("Record not found: " + issueId));
        String fileName = secondary ? issue.getSecondaryEvidenceUrl() : issue.getEvidenceUrl();
        return fileName == null ? null : fileUploadService.loadHandoverImage(fileName);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
    }

    private Employee requireHandler(Booking booking, Long employeeId) {
        Employee actor = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));
        boolean assigned = booking.getEmployee() != null && employeeId.equals(booking.getEmployee().getId());
        if (!assigned && !"admin".equals(actor.getRole())) {
            throw new IllegalStateException("Only the assigned staff member or an admin can perform this action");
        }
        return actor;
    }

    private void requireOdometer(Vehicle vehicle, Integer odometer) {
        if (odometer == null || odometer < vehicle.getOdometer()) {
            throw new IllegalArgumentException("Odometer cannot be lower than the current reading of "
                    + vehicle.getOdometer() + " km");
        }
    }

    private List<SurchargeDTO> filledSurcharges(CheckOutDTO form) {
        List<SurchargeDTO> filled = new ArrayList<>();
        for (SurchargeDTO surcharge : form.getSurcharges()) {
            if (surcharge == null || surcharge.getAmount() == null || surcharge.getAmount().signum() == 0) {
                continue;
            }
            if (surcharge.getAmount().signum() < 0) {
                throw new IllegalArgumentException("Surcharge cannot be negative");
            }
            if (surcharge.getFeeType() == null || !SURCHARGE_TYPES.contains(surcharge.getFeeType())) {
                throw new IllegalArgumentException("Unknown surcharge type");
            }
            filled.add(surcharge);
        }
        return filled;
    }

    private BigDecimal finalAmount(Booking booking) {
        return booking.getTotalPrice()
                .add(bookingFeeService.calculateTotalAdditionalFees(booking.getId()))
                .subtract(booking.getDepositAmount());
    }

    private Issue newIssue(Booking booking, Employee actor, String type, String status, String subject) {
        Issue issue = new Issue();
        issue.setBooking(booking);
        issue.setSlot(booking.getSlot());
        issue.setVehicle(booking.getVehicle());
        issue.setEmployee(actor);
        issue.setIssueType(type);
        issue.setStatus(status);
        issue.setSubject(subject);
        issue.setReportedAt(OffsetDateTime.now());
        return issue;
    }

    private String subject(String label, Integer odometer, Integer fuelPercent, String notes) {
        String subject = label + ": ODO " + odometer + " km, fuel " + fuelPercent + "%";
        return hasText(notes) ? subject + ", " + notes.trim() : subject;
    }

    public static final int MAX_EARLY_PICKUP_MINUTES = 30;

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
    }

    private boolean isMissing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}