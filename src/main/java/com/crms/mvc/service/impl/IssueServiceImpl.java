package com.crms.mvc.service.impl;

import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.dto.IssueStatsDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueEvidence;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.IssueCancelledEvent;
import com.crms.mvc.event.IssueFeeDueEvent;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueEvidenceRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.IssueSpecifications;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class IssueServiceImpl implements IssueService {

    private static final int FEE_DESCRIPTION_MAX = 255;
    private static final int MAX_PHOTOS = 10;

    private final IssueRepository issueRepository;
    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final BookingFeeRepository bookingFeeRepository;
    private final IssueEvidenceRepository issueEvidenceRepository;
    private final FileUploadService fileUploadService;
    private final LicensePlateRepository licensePlateRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public IssueServiceImpl(IssueRepository issueRepository,
                            BookingRepository bookingRepository,
                            EmployeeRepository employeeRepository,
                            BookingFeeRepository bookingFeeRepository,
                            IssueEvidenceRepository issueEvidenceRepository,
                            FileUploadService fileUploadService,
                            LicensePlateRepository licensePlateRepository,
                            ApplicationEventPublisher eventPublisher) {
        this(issueRepository, bookingRepository, employeeRepository, bookingFeeRepository,
                issueEvidenceRepository, fileUploadService, licensePlateRepository, null, eventPublisher);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public IssueServiceImpl(IssueRepository issueRepository,
                            BookingRepository bookingRepository,
                            EmployeeRepository employeeRepository,
                            BookingFeeRepository bookingFeeRepository,
                            IssueEvidenceRepository issueEvidenceRepository,
                            FileUploadService fileUploadService,
                            LicensePlateRepository licensePlateRepository,
                            PaymentRepository paymentRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.issueRepository = issueRepository;
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.bookingFeeRepository = bookingFeeRepository;
        this.issueEvidenceRepository = issueEvidenceRepository;
        this.fileUploadService = fileUploadService;
        this.licensePlateRepository = licensePlateRepository;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Issue create(IssueFormDTO form, IssueActor actor) {
        Booking booking = bookingRepository.findById(form.getBookingId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + form.getBookingId()));
        if (!isRentalStarted(booking) || booking.getVehicle() == null) {
            throw new IllegalStateException(
                    "Chỉ có thể ghi nhận sự cố cho đơn thuê khách đã nhận xe (đang hoạt động hoặc đã trả)");
        }
        BigDecimal fee = requireFeePositive(form.getFeeAmount());
        Employee reporter = employeeRepository.findById(actor.employeeId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên: " + actor.employeeId()));

        Issue issue = new Issue();
        issue.setBooking(booking);
        issue.setVehicle(booking.getVehicle());
        issue.setEmployee(reporter);
        issue.setStatus(IssueStatus.OPEN);
        issue.setReportedAt(OffsetDateTime.now());
        applyDetails(issue, form);
        Issue saved = issueRepository.save(issue);

        syncFee(saved, fee);
        eventPublisher.publishEvent(new IssueFeeDueEvent(saved.getId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking findBookingForViolation(String plate, LocalDateTime occurredAt) {
        OffsetDateTime at = occurredAt.atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
        LicensePlate onTheCar = licensePlateRepository.findByPlateIgnoreCase(plate == null ? "" : plate.trim())
                .filter(p -> !p.getValidFrom().isAfter(at) && (p.getValidTo() == null || p.getValidTo().isAfter(at)))
                .orElseThrow(() -> new NoSuchElementException("Không có xe nào mang biển số này tại thời điểm vi phạm"));

        return bookingRepository.findRentalsCovering(onTheCar.getVehicle().getId(), at).stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Không có đơn thuê nào của xe này tại thời điểm vi phạm"));
    }

    @Override
    public Issue update(Long issueId, IssueFormDTO form) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sự cố: " + issueId));
        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalStateException("Chỉ có thể chỉnh sửa sự cố đang mở");
        }
        BigDecimal fee = requireFeePositive(form.getFeeAmount());

        applyDetails(issue, form);
        Issue saved = issueRepository.save(issue);

        // Hủy phiên thanh toán pending cũ (nếu có) để tránh khách trả theo mức giá cũ
        invalidatePendingPayments(issue.getBooking().getId());

        syncFee(saved, fee);
        eventPublisher.publishEvent(new IssueFeeDueEvent(saved.getId()));
        return saved;
    }

    @Override
    public Issue settleDirectly(Long issueId, String paymentMethod, String note, IssueActor actor) {
        Issue issue = load(issueId);
        requireStatus(issue, IssueStatus.OPEN, "Chỉ có thể thu tiền cho sự cố đang mở");
        BookingFee fee = findFee(issue.getId())
                .orElseThrow(() -> new IllegalStateException("Sự cố này không có khoản phí bồi thường"));

        invalidatePendingPayments(issue.getBooking().getId());

        String method = (paymentMethod != null && !paymentMethod.isBlank())
                ? paymentMethod.trim().toUpperCase() : Payment.METHOD_BANK_TRANSFER;
        if (!Payment.METHOD_BANK_TRANSFER.equals(method) && !"CASH".equals(method)) {
            method = "CASH";
        }

        Payment payment = new Payment();
        payment.setBooking(issue.getBooking());
        payment.setPaymentType(Payment.TYPE_ISSUE_COMPENSATION);
        payment.setPaymentMethod(method);
        payment.setAmount(fee.getAmount());
        payment.setStatus(Payment.STATUS_PAID);
        payment.setPaidAt(OffsetDateTime.now());
        payment.setTransactionCode("DIRECT-" + issue.getId() + "-" + System.currentTimeMillis());
        Payment savedPayment = paymentRepository != null ? paymentRepository.save(payment) : payment;

        fee.setPayment(savedPayment);
        bookingFeeRepository.save(fee);

        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolutionNote(note != null && !note.isBlank() ? note.trim() : "Đã thanh toán trực tiếp tại quầy (" + method + ")");
        issue.setResolvedAt(OffsetDateTime.now());
        return issueRepository.save(issue);
    }

    @Override
    public Issue resolve(Long issueId, String resolutionNote, IssueActor actor) {
        Issue issue = load(issueId);
        requireStatus(issue, IssueStatus.OPEN, "Chỉ có thể chốt sự cố đang mở");
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolutionNote(resolutionNote != null && !resolutionNote.isBlank() ? resolutionNote.trim() : "Đã giải quyết");
        issue.setResolvedAt(OffsetDateTime.now());
        return issueRepository.save(issue);
    }

    @Override
    public Issue cancel(Long issueId, String reason, IssueActor actor) {
        Issue issue = load(issueId);
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new IllegalStateException("Sự cố này đã được huỷ trước đó");
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            throw new IllegalStateException("Không thể huỷ sự cố đã hoàn tất thanh toán");
        }
        String text = requireText(reason, "Vui lòng nhập lý do huỷ sự cố");

        invalidatePendingPayments(issue.getBooking().getId());

        issue.setStatus(IssueStatus.CANCELLED);
        issue.setCancelReason(text);
        findFee(issue.getId()).ifPresent(bookingFeeRepository::delete);
        Issue saved = issueRepository.save(issue);

        eventPublisher.publishEvent(new IssueCancelledEvent(saved.getId(), text));
        return saved;
    }

    @Override
    public Issue reopen(Long issueId, IssueActor actor) {
        throw new IllegalStateException("Không thể mở lại sự cố đã hoàn tất thanh toán hoặc đã huỷ");
    }

    @Override
    public IssueEvidence addEvidence(Long issueId, MultipartFile image) {
        Issue issue = load(issueId);
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new IllegalStateException("Không thể thêm ảnh vào sự cố đã huỷ");
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            throw new IllegalStateException("Không thể thêm ảnh vào sự cố đã hoàn tất thanh toán");
        }
        requireFeeNotTaken(issue);
        if (issueEvidenceRepository.countByIssueId(issueId) >= MAX_PHOTOS) {
            throw new IllegalStateException("Một sự cố chỉ được có tối đa " + MAX_PHOTOS + " ảnh bằng chứng");
        }

        String fileName = fileUploadService.saveIssueEvidence(image);
        IssueEvidence evidence = new IssueEvidence();
        evidence.setIssue(issue);
        evidence.setFileName(fileName);
        evidence.setUploadedAt(OffsetDateTime.now());
        return issueEvidenceRepository.save(evidence);
    }

    @Override
    public void removeEvidence(Long issueId, Long evidenceId) {
        Issue issue = load(issueId);
        IssueEvidence evidence = findEvidence(issueId, evidenceId);
        requireStatus(issue, IssueStatus.OPEN, "Chỉ có thể xoá ảnh khi sự cố đang mở");

        issueEvidenceRepository.delete(evidence);
        fileUploadService.deleteIssueEvidence(evidence.getFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadEvidence(Long issueId, Long evidenceId) {
        return readEvidenceFile(issueId, evidenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadEvidenceForCustomer(Long issueId, Long evidenceId, Long customerId) {
        Issue issue = load(issueId);
        Long ownerId = issue.getBooking().getCustomer().getId();
        if (customerId == null || !customerId.equals(ownerId) || issue.getStatus() == IssueStatus.CANCELLED) {
            throw new NoSuchElementException("Không tìm thấy sự cố: " + issueId);
        }
        return readEvidenceFile(issueId, evidenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IssueDTO> findDetail(Long issueId) {
        return issueRepository.findByIdWithDetails(issueId)
                .map(issue -> toDto(issue, findFee(issue.getId()).orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueDTO> findByBooking(Long bookingId) {
        List<Issue> issues = issueRepository.findByBookingIdWithDetails(bookingId);
        Map<Long, BookingFee> feesByIssueId = fetchFeesByIssueIds(issues.stream().map(Issue::getId).toList());
        return issues.stream()
                .map(issue -> toDto(issue, feesByIssueId.get(issue.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueDTO> findByBookingForCustomer(Long bookingId, Long customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        if (customerId == null || !customerId.equals(booking.getCustomer().getId())) {
            throw new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId);
        }
        List<Issue> issues = issueRepository.findByBookingIdWithDetails(bookingId);
        Map<Long, BookingFee> feesByIssueId = fetchFeesByIssueIds(issues.stream().map(Issue::getId).toList());
        return issues.stream()
                .filter(issue -> issue.getStatus() != IssueStatus.CANCELLED)
                .map(issue -> toDto(issue, feesByIssueId.get(issue.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean bookingHasVehicleDamage(Long bookingId) {
        return issueRepository.existsByBookingIdAndIssueTypeInAndStatusNot(
                bookingId, List.of(IssueType.COLLISION, IssueType.DAMAGE), IssueStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IssueDTO> searchForAdmin(IssueQueryDTO query) {
        Sort newestFirst = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
        Specification<Issue> filters = IssueSpecifications.matching(query);

        Page<Issue> page = issueRepository.findAll(filters, PageRequest.of(query.getPageIndex(), query.getSize(), newestFirst));
        if (page.isEmpty() && page.getTotalPages() > 0) {
            page = issueRepository.findAll(filters, PageRequest.of(page.getTotalPages() - 1, query.getSize(), newestFirst));
        }

        Map<Long, BookingFee> feesByIssueId = fetchFeesByIssueIds(page.getContent().stream().map(Issue::getId).toList());
        return page.map(issue -> toDto(issue, feesByIssueId.get(issue.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public IssueStatsDTO stats() {
        ZonedDateTime monthStart = ZonedDateTime.now(DateTimeUtils.VIETNAM_ZONE)
                .withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return new IssueStatsDTO(
                issueRepository.countByStatus(IssueStatus.OPEN),
                bookingFeeRepository.countIssueFeesAwaitingPayment(IssueStatus.RESOLVED),
                bookingFeeRepository.sumIssueFeesPaidBetween(
                        monthStart.toOffsetDateTime(), monthStart.plusMonths(1).toOffsetDateTime()));
    }

    private void invalidatePendingPayments(Long bookingId) {
        if (bookingId == null || paymentRepository == null) return;
        paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                bookingId, Payment.TYPE_ISSUE_COMPENSATION, Payment.STATUS_PENDING)
                .ifPresent(p -> {
                    p.setStatus(Payment.STATUS_FAILED);
                    paymentRepository.save(p);
                    bookingFeeRepository.findByPaymentId(p.getId()).forEach(fee -> {
                        fee.setPayment(null);
                        bookingFeeRepository.save(fee);
                    });
                });
    }

    private Map<Long, BookingFee> fetchFeesByIssueIds(Collection<Long> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Map.of();
        }
        return bookingFeeRepository.findByIssueIdIn(issueIds).stream()
                .filter(f -> f.getIssue() != null)
                .collect(Collectors.toMap(f -> f.getIssue().getId(), f -> f, (f1, f2) -> f1));
    }

    private IssueEvidence findEvidence(Long issueId, Long evidenceId) {
        return issueEvidenceRepository.findByIdAndIssueId(evidenceId, issueId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ảnh: " + evidenceId));
    }

    private Resource readEvidenceFile(Long issueId, Long evidenceId) {
        Resource file = fileUploadService.loadIssueEvidence(findEvidence(issueId, evidenceId).getFileName());
        if (file == null) {
            throw new NoSuchElementException("Không tìm thấy tệp ảnh: " + evidenceId);
        }
        return file;
    }

    private Issue load(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sự cố: " + issueId));
    }

    private void requireStatus(Issue issue, IssueStatus expected, String message) {
        if (issue.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private String requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private void requireFeeNotTaken(Issue issue) {
        boolean taken = findFee(issue.getId()).map(this::isTakenByPayment).orElse(false);
        if (taken) {
            throw new IllegalStateException("Khoản phí của sự cố này đang trong phiên thanh toán hoặc đã được thanh toán");
        }
    }

    private boolean isTakenByPayment(BookingFee fee) {
        Payment payment = fee.getPayment();
        if (payment == null || Payment.STATUS_FAILED.equalsIgnoreCase(payment.getStatus())) {
            return false;
        }
        if (Payment.STATUS_PAID.equalsIgnoreCase(payment.getStatus())) {
            return true;
        }
        return !payment.isExpired();
    }

    private boolean isRentalStarted(Booking booking) {
        return Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus())
                || Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus());
    }

    private void applyDetails(Issue issue, IssueFormDTO form) {
        issue.setIssueType(form.getIssueType());
        issue.setSubject(form.getSubject().trim());
        issue.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        OffsetDateTime occurred = form.getOccurredAt().atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
        if (occurred.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Thời gian sự cố không được ở tương lai");
        }
        if (issue.getBooking() != null && issue.getBooking().getPickupTime() != null
                && occurred.isBefore(issue.getBooking().getPickupTime())) {
            throw new IllegalArgumentException("Thời gian sự cố không được trước thời gian nhận xe của chuyến thuê");
        }
        issue.setOccurredAt(occurred);
    }

    private BigDecimal requireFeePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức phí bồi thường phải lớn hơn 0");
        }
        return amount;
    }

    private void syncFee(Issue issue, BigDecimal amount) {
        Optional<BookingFee> existing = findFee(issue.getId());
        BookingFee fee = existing.orElseGet(() -> {
            BookingFee created = new BookingFee();
            created.setBooking(issue.getBooking());
            created.setIssue(issue);
            created.setFeeType(BookingFee.FEE_TYPE_ISSUE_COMPENSATION);
            created.setCreatedAt(OffsetDateTime.now());
            return created;
        });
        fee.setAmount(amount);
        fee.setDescription(describe(issue));
        bookingFeeRepository.save(fee);
    }

    private Optional<BookingFee> findFee(Long issueId) {
        if (issueId == null) {
            return Optional.empty();
        }
        return bookingFeeRepository.findByIssueId(issueId).stream().findFirst();
    }

    private String describe(Issue issue) {
        String text = issue.getIssueType().getLabel() + ": " + issue.getSubject();
        return text.length() <= FEE_DESCRIPTION_MAX ? text : text.substring(0, FEE_DESCRIPTION_MAX);
    }

    private IssueDTO toDto(Issue issue, BookingFee fee) {
        IssueDTO dto = new IssueDTO();
        dto.setId(issue.getId());
        dto.setBookingId(issue.getBooking() != null ? issue.getBooking().getId() : null);
        dto.setVehicleId(issue.getVehicle() != null ? issue.getVehicle().getId() : null);

        String plate = null;
        if (issue.getVehicle() != null) {
            plate = licensePlateRepository.findCarriedAt(issue.getVehicle().getId(), issue.getOccurredAt())
                    .map(LicensePlate::getPlate)
                    .orElseGet(() -> issue.getVehicle().getLicensePlate() != null
                            ? issue.getVehicle().getLicensePlate().getPlate() : null);
        }
        dto.setLicensePlate(plate);
        dto.setVehicleName(issue.getVehicle() != null && issue.getVehicle().getVehicleType() != null
                ? issue.getVehicle().getVehicleType().getName() : null);
        dto.setCustomerName(issue.getBooking() != null && issue.getBooking().getCustomer() != null
                ? issue.getBooking().getCustomer().getName() : null);
        dto.setReporterName(issue.getEmployee() != null ? issue.getEmployee().getName() : null);
        dto.setIssueType(issue.getIssueType());
        dto.setSubject(issue.getSubject());
        dto.setDescription(issue.getDescription());
        dto.setStatus(issue.getStatus());
        dto.setOccurredAt(issue.getOccurredAt());
        dto.setReportedAt(issue.getReportedAt());
        dto.setResolvedAt(issue.getResolvedAt());
        dto.setResolutionNote(issue.getResolutionNote());
        dto.setCancelReason(issue.getCancelReason());
        if (fee != null) {
            dto.setFeeAmount(fee.getAmount());
            Payment payment = fee.getPayment();
            dto.setFeePaid(payment != null && Payment.STATUS_PAID.equalsIgnoreCase(payment.getStatus()));
            dto.setFeeLocked(isTakenByPayment(fee));
        } else {
            dto.setFeeAmount(null);
            dto.setFeePaid(false);
            dto.setFeeLocked(false);
        }
        List<IssueEvidence> evidences = issue.getEvidences();
        if (evidences != null) {
            dto.setEvidences(evidences.stream().map(e -> new IssueDTO.EvidenceDTO(e.getId(), e.getFileName(), e.getUploadedAt())).toList());
        }
        return dto;
    }
}
