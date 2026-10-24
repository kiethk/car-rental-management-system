package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Contract;
import com.crms.mvc.entity.ContractTemplate;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.ContractRepository;
import com.crms.mvc.repository.ContractTemplateRepository;
import com.crms.mvc.repository.LicenseDocumentRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.ContractDocumentGenerator;
import com.crms.mvc.service.ContractMailService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    // A contract is only issued once the deposit is paid.
    private static final Set<String> ISSUABLE_BOOKING_STATUSES = Set.of("confirmed", "active", "completed");

    private final ContractRepository contractRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final LicenseDocumentRepository licenseDocumentRepository;
    private final PaymentRepository paymentRepository;
    private final ContractDocumentGenerator contractDocumentGenerator;
    private final ContractMailService contractMailService;
    private final FileUploadService fileUploadService;

    public ContractServiceImpl(ContractRepository contractRepository,
                               ContractTemplateRepository contractTemplateRepository,
                               LicenseDocumentRepository licenseDocumentRepository,
                               PaymentRepository paymentRepository,
                               ContractDocumentGenerator contractDocumentGenerator,
                               ContractMailService contractMailService,
                               FileUploadService fileUploadService) {
        this.contractRepository = contractRepository;
        this.contractTemplateRepository = contractTemplateRepository;
        this.licenseDocumentRepository = licenseDocumentRepository;
        this.paymentRepository = paymentRepository;
        this.contractDocumentGenerator = contractDocumentGenerator;
        this.contractMailService = contractMailService;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public void createContract(Booking booking, Long templateId, String acceptedIp) {
        ContractTemplate template = contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Contract template not found: " + templateId));

        OffsetDateTime now = OffsetDateTime.now();
        Contract contract = new Contract();
        contract.setContractNumber("CTR-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", booking.getId()));
        contract.setBooking(booking);
        contract.setTemplate(template);
        contract.setAcceptedAt(now);
        contract.setAcceptedIp(acceptedIp);
        contract.setCreatedAt(now);
        contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractDTO> findByBookingId(Long bookingId) {
        return contractRepository.findByBookingId(bookingId).map(ContractDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplateDTO getTermsOfBooking(Long bookingId) {
        return ContractTemplateDTO.fromEntity(findContract(bookingId).getTemplate());
    }

    @Override
    public void issueContract(Long bookingId) {
        Contract contract = findContract(bookingId);
        if (!ISSUABLE_BOOKING_STATUSES.contains(contract.getBooking().getStatus())) {
            throw new IllegalStateException("Contract can only be issued after the deposit is paid");
        }

        ContractDocumentDTO document;
        byte[] pdf;
        if (!contract.isIssued()) {
            contract.setIssuedAt(OffsetDateTime.now());
            document = buildDocument(contract);
            pdf = contractDocumentGenerator.generatePdf(document);
            contract.setFileName(fileUploadService.saveContractPdf(contract.getContractNumber(), pdf));
        } else if (contract.getEmailedAt() == null) {
            document = buildDocument(contract);
            pdf = readIssuedPdf(contract);
        } else {
            return;
        }

        try {
            contractMailService.sendContract(document, pdf);
            contract.setEmailedAt(OffsetDateTime.now());
        } catch (RuntimeException exception) {
            // Email is optional: the PDF is already stored and staff can resend from the booking page.
            log.warn("Contract {} was issued but not emailed. {}", contract.getContractNumber(), exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadContractPdf(Long bookingId) {
        Contract contract = findContract(bookingId);
        if (!contract.isIssued()) {
            throw new NoSuchElementException("Contract " + contract.getContractNumber() + " has not been issued yet");
        }
        Resource file = fileUploadService.loadContractPdf(contract.getFileName());
        if (file == null) {
            throw new NoSuchElementException("Contract file is missing: " + contract.getContractNumber());
        }
        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadContractPdfForCustomer(Long bookingId, Long customerId) {
        Contract contract = findContract(bookingId);
        if (!contract.getBooking().getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized access to contract");
        }
        return loadContractPdf(bookingId);
    }

    private Contract findContract(Long bookingId) {
        return contractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Contract not found for booking: " + bookingId));
    }

    private byte[] readIssuedPdf(Contract contract) {
        try {
            return loadContractPdf(contract.getBooking().getId()).getContentAsByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read contract file " + contract.getContractNumber(), exception);
        }
    }

    private ContractDocumentDTO buildDocument(Contract contract) {
        Booking booking = contract.getBooking();
        Customer customer = booking.getCustomer();
        VehicleType type = booking.getSlot().getVehicleType();
        Vehicle vehicle = booking.getVehicle();

        ContractDocumentDTO doc = new ContractDocumentDTO();
        doc.setContractNumber(contract.getContractNumber());
        doc.setBookingId(booking.getId());
        doc.setIssuedAt(toVietnamTime(contract.getIssuedAt()));

        doc.setCustomerName(customer.getName());
        doc.setCustomerPhone(customer.getPhone());
        doc.setCustomerEmail(customer.getEmail());
        licenseDocumentRepository.findByCustomer_IdOrderBySubmittedAtDesc(customer.getId()).stream()
                .filter(license -> "approved".equals(license.getStatus()))
                .findFirst()
                .ifPresent(license -> fillLicense(doc, license));

        doc.setVehicleName(type.getDisplayName());
        doc.setVehicleCategory(type.getCategory());
        doc.setSeats(type.getSeats());
        doc.setTransmission(type.getTransmission());
        doc.setFuelType(type.getFuelType());
        if (vehicle != null) {
            doc.setLicensePlate(vehicle.getLicensePlate() != null ? vehicle.getLicensePlate().getPlate() : null);
            doc.setVehicleColor(vehicle.getColor());
            doc.setVehicleYear(vehicle.getYear());
        }

        doc.setPickupTime(toVietnamTime(booking.getPickupTime()));
        doc.setScheduledEndTime(toVietnamTime(booking.getScheduledEndTime()));
        // Same hour rule as BookingServiceImpl.createBooking, so the printed amounts match what was charged.
        long hours = Math.max(1, Duration.between(booking.getPickupTime(), booking.getScheduledEndTime()).toHours());
        doc.setRentalHours(hours);

        doc.setPricePerHour(booking.getPricePerHour());
        doc.setRentalPrice(booking.getPricePerHour().multiply(BigDecimal.valueOf(hours)));
        doc.setDiscountAmount(booking.getDiscountAmount());
        doc.setPromotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null);
        doc.setTotalPrice(booking.getTotalPrice());
        doc.setDepositAmount(booking.getDepositAmount());
        doc.setRemainingAmount(booking.getTotalPrice().subtract(booking.getDepositAmount()));
        paymentRepository.findByBookingIdAndPaymentType(booking.getId(), "DEPOSIT").ifPresent(payment -> {
            doc.setDepositTransactionCode(payment.getTransactionCode());
            doc.setDepositPaidAt(toVietnamTime(payment.getPaidAt()));
        });

        doc.setAcceptedAt(toVietnamTime(contract.getAcceptedAt()));
        doc.setAcceptedIp(contract.getAcceptedIp());
        doc.setTerms(ContractTemplateDTO.fromEntity(contract.getTemplate()));
        return doc;
    }

    private void fillLicense(ContractDocumentDTO doc, LicenseDocument license) {
        doc.setLicenseNumber(license.getLicenseNumber());
        doc.setLicenseClass(license.getLicenseClass());
        doc.setLicenseExpiry(license.getLicenseExpiry());
    }

    private OffsetDateTime toVietnamTime(OffsetDateTime time) {
        return time != null ? time.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime() : null;
    }
}
