package com.crms.mvc.service.impl;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.SlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final SlotService slotService;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentService paymentService;
    private final ContractService contractService;
    private final ContractTemplateService contractTemplateService;
    private final BookingFeeService bookingFeeService;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            SlotService slotService,
            VehicleTypeRepository vehicleTypeRepository,
            VehicleRepository vehicleRepository,
            EmployeeRepository employeeRepository,
            PromotionRepository promotionRepository,
            PaymentService paymentService,
            ContractService contractService,
            ContractTemplateService contractTemplateService,
            BookingFeeService bookingFeeService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.slotService = slotService;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.promotionRepository = promotionRepository;
        this.paymentService = paymentService;
        this.contractService = contractService;
        this.contractTemplateService = contractTemplateService;
        this.bookingFeeService = bookingFeeService;
    }

    @Override
    public Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().isEqual(dto.getEndTime())) {
            throw new IllegalStateException("Return time must be after pickup time");
        }

        ZoneOffset defaultOffset = ZoneOffset.ofHours(7); // UTC+7 for Vietnam
        OffsetDateTime start = dto.getStartTime().atOffset(defaultOffset);
        OffsetDateTime end = dto.getEndTime().atOffset(defaultOffset);

        VehicleType type = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new NoSuchElementException("VehicleType not found: " + dto.getVehicleTypeId()));

        // Clickwrap: the customer is bound only by the version they saw on the checkout page.
        ContractTemplateDTO terms = contractTemplateService.getActiveTemplate();
        if (!terms.getId().equals(dto.getContractTemplateId())) {
            throw new IllegalStateException("The rental terms were updated while you were checking out. "
                    + "Please review and accept the new version.");
        }

        // 0. Tự động hủy các đơn pending quá hạn (15 phút) của toàn hệ thống
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));

        // 0.1 Nếu khách hàng này đã có đơn 'pending_payment' trong cùng khung giờ,
        // hệ thống tự hủy đơn pending cũ đó để nhường chỗ cho lần tạo mới này!
        List<Booking> existingPendingBookings = bookingRepository.findPendingBookingsByCustomerAndTimeRange(customerId,
                start, end);
        for (Booking oldPending : existingPendingBookings) {
            oldPending.setStatus("cancelled");
            oldPending.setCancelledAt(OffsetDateTime.now());
            bookingRepository.save(oldPending);
        }

        // 1. Tìm xe thực tế có sẵn thông qua SlotService
        List<Vehicle> availableVehicles = slotService.findAvailableVehiclesInTimeRange(type.getId(), start, end);
        Vehicle assignedVehicle = null;
        if (!availableVehicles.isEmpty()) {
            assignedVehicle = availableVehicles.get(0);
        }

        // 2. Tạo Slot mới tương ứng
        Slot slot = slotService.createSlotForBooking(type.getId(), start, end);

        // 3. Tính toán chi phí
        long hours = Math.max(1, Duration.between(start, end).toHours());
        BigDecimal rentalPrice = type.getPricePerHour().multiply(BigDecimal.valueOf(hours));
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion appliedPromo = null;

        if (dto.getPromotionCode() != null && !dto.getPromotionCode().trim().isEmpty()) {
            appliedPromo = promotionRepository.findByCodeIgnoreCase(dto.getPromotionCode().trim()).orElse(null);
            if (appliedPromo != null && appliedPromo.getQuantity() > 0) {
                if ("percent".equalsIgnoreCase(appliedPromo.getDiscountType())) {
                    BigDecimal rate = appliedPromo.getDiscountValue().divide(BigDecimal.valueOf(100));
                    discountAmount = rentalPrice.multiply(rate);
                } else {
                    discountAmount = appliedPromo.getDiscountValue();
                }
                if (discountAmount.compareTo(rentalPrice) > 0) {
                    discountAmount = rentalPrice;
                }
            }
        }

        BigDecimal totalPrice = rentalPrice.subtract(discountAmount);
        BigDecimal depositAmount = totalPrice.multiply(terms.getDepositRate()).setScale(0, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSlot(slot);
        booking.setVehicle(assignedVehicle);
        booking.setPromotion(appliedPromo);
        booking.setPickupTime(start);
        booking.setScheduledEndTime(end);
        booking.setPricePerHour(type.getPricePerHour());
        booking.setDepositAmount(depositAmount);
        booking.setDiscountAmount(discountAmount);
        booking.setLateFee(BigDecimal.ZERO);
        booking.setTotalPrice(totalPrice);
        booking.setStatus("pending_payment");
        booking.setCreatedAt(OffsetDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // Lưu bằng chứng khách đã đồng ý đúng phiên bản điều khoản; PDF được phát hành sau khi cọc
        contractService.createContract(saved, terms.getId(), clientIp);

        return saved;
    }

    @Override
    @Transactional
    public List<BookingSummaryDTO> getMyBookings(Long customerId) {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(b -> {
            BookingSummaryDTO s = new BookingSummaryDTO();
            s.setId(b.getId());
            VehicleType vt = b.getSlot().getVehicleType();
            s.setVehicleTypeName(vt.getName());
            s.setVehicleCategory(vt.getCategory());
            s.setImageUrl(resolveImageUrl(vt));
            s.setLicensePlate(resolvePlate(b));
            s.setPickupTime(b.getPickupTime());
            s.setScheduledEndTime(b.getScheduledEndTime());
            s.setTotalPrice(b.getTotalPrice());
            s.setDepositAmount(b.getDepositAmount());
            s.setStatus(b.getStatus());
            s.setCustomerName(b.getCustomer() != null ? b.getCustomer().getName() : null);
            return s;
        }).toList();
    }

    @Override
    @Transactional
    public BookingDetailDTO getBookingDetail(Long bookingId, Long customerId) {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized access to booking");
        }
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional
    public BookingDetailDTO getBookingDetailForAdmin(Long bookingId) {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional
    public List<BookingSummaryDTO> getAllBookings(String status) {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));
        List<Booking> list = (status == null || status.isBlank())
                ? bookingRepository.findAllByOrderByCreatedAtDesc()
                : bookingRepository.findByStatusOrderByCreatedAtDesc(status.trim());

        return list.stream().map(b -> {
            BookingSummaryDTO s = new BookingSummaryDTO();
            s.setId(b.getId());
            VehicleType vt = b.getSlot().getVehicleType();
            s.setVehicleTypeName(vt.getName());
            s.setVehicleCategory(vt.getCategory());
            s.setImageUrl(resolveImageUrl(vt));
            s.setLicensePlate(resolvePlate(b));
            s.setPickupTime(b.getPickupTime());
            s.setScheduledEndTime(b.getScheduledEndTime());
            s.setTotalPrice(b.getTotalPrice());
            s.setDepositAmount(b.getDepositAmount());
            s.setStatus(b.getStatus());
            s.setCustomerName(b.getCustomer() != null ? b.getCustomer().getName() : null);
            return s;
        }).toList();
    }

    @Override
    public void cancelBooking(Long bookingId, Long customerId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized action");
        }
        if (!"pending_payment".equalsIgnoreCase(b.getStatus()) && !"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Cannot cancel booking in status: " + b.getStatus());
        }

        b.setStatus("cancelled");
        b.setCancelledAt(OffsetDateTime.now());
        bookingRepository.save(b);
    }

    @Override
    public void assignVehicle(Long bookingId, Long vehicleId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + vehicleId));

        if (!v.getVehicleType().getId().equals(b.getSlot().getVehicleType().getId())) {
            throw new IllegalStateException("Vehicle type does not match booking request");
        }
        if (!"available".equalsIgnoreCase(v.getStatus())) {
            throw new IllegalStateException("Vehicle is currently not available: " + v.getStatus());
        }

        b.setVehicle(v);
        bookingRepository.save(b);
    }

    @Override
    public void checkIn(Long bookingId, Long employeeId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));

        if (b.getVehicle() == null) {
            throw new IllegalStateException("Cannot check-in: Vehicle is not assigned yet");
        }
        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Booking must be confirmed before check-in");
        }

        b.setStatus("active");
        b.setEmployee(emp);
        b.getVehicle().setStatus("rented");
        // Handover: record the plate that is physically on the car right now
        if (b.getVehicle().getLicensePlate() != null) {
            b.setLicensePlate(b.getVehicle().getLicensePlate().getPlate());
        }

        vehicleRepository.save(b.getVehicle());
        bookingRepository.save(b);
    }

    @Override
    public void checkOut(Long bookingId, Long employeeId, CheckOutDTO dto) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!"active".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Booking is not currently active");
        }

        OffsetDateTime actualReturn = OffsetDateTime.now();
        b.setActualReturnTime(actualReturn);

        BigDecimal lateFee = BigDecimal.ZERO;
        if (actualReturn.isAfter(b.getScheduledEndTime())) {
            long lateHours = Math.max(1, Duration.between(b.getScheduledEndTime(), actualReturn).toHours());
            // Hệ số phạt lấy từ phiên bản điều khoản khách đã đồng ý, không lấy bản đang áp dụng
            BigDecimal multiplier = contractService.getTermsOfBooking(bookingId).getLateFeeMultiplier();
            lateFee = b.getPricePerHour().multiply(BigDecimal.valueOf(lateHours)).multiply(multiplier);
        }

        paymentService.processSettlement(bookingId, lateFee, dto != null ? dto.getAdditionalFine() : BigDecimal.ZERO);

        if (b.getVehicle() != null) {
            b.getVehicle().setStatus("available");
            vehicleRepository.save(b.getVehicle());
        }
    }

    private BookingDetailDTO mapToDetailDTO(Booking b) {
        BookingDetailDTO d = new BookingDetailDTO();
        d.setId(b.getId());
        d.setCustomerId(b.getCustomer().getId());
        d.setCustomerName(b.getCustomer().getName());
        d.setCustomerPhone(b.getCustomer().getPhone());
        d.setCustomerEmail(b.getCustomer().getEmail());

        VehicleType vt = b.getSlot().getVehicleType();
        d.setVehicleTypeId(vt.getId());
        d.setVehicleTypeName(vt.getName());
        d.setVehicleCategory(vt.getCategory());
        d.setSeats(vt.getSeats());
        d.setImageUrl(resolveImageUrl(vt));

        if (b.getVehicle() != null) {
            d.setVehicleId(b.getVehicle().getId());
            d.setVehicleColor(b.getVehicle().getColor());
        }
        d.setLicensePlate(resolvePlate(b));

        d.setSlotId(b.getSlot().getId());
        d.setPickupTime(b.getPickupTime());
        d.setScheduledEndTime(b.getScheduledEndTime());
        d.setActualReturnTime(b.getActualReturnTime());

        d.setPricePerHour(b.getPricePerHour());
        d.setDepositAmount(b.getDepositAmount());
        d.setDiscountAmount(b.getDiscountAmount());
        d.setLateFee(b.getLateFee());
        d.setTotalPrice(b.getTotalPrice());
        d.setPromotionCode(b.getPromotion() != null ? b.getPromotion().getCode() : null);

        d.setStatus(b.getStatus());
        d.setCreatedAt(b.getCreatedAt());

        List<Payment> pList = paymentService.getPaymentsByBooking(b.getId());
        List<BookingDetailDTO.PaymentDTO> pDtos = new ArrayList<>();
        for (Payment p : pList) {
            BookingDetailDTO.PaymentDTO pd = new BookingDetailDTO.PaymentDTO();
            pd.setId(p.getId());
            pd.setTransactionCode(p.getTransactionCode());
            pd.setPaymentType(p.getPaymentType());
            pd.setAmount(p.getAmount());
            pd.setPaymentMethod(p.getPaymentMethod());
            pd.setStatus(p.getStatus());
            pd.setPaidAt(p.getPaidAt());
            pDtos.add(pd);
        }
        d.setPayments(pDtos);

        contractService.findByBookingId(b.getId()).ifPresent(d::setContract);

        List<BookingFee> fees = bookingFeeService.getFeesByBooking(b.getId());
        List<BookingDetailDTO.BookingFeeDTO> feeDtos = new ArrayList<>();
        for (BookingFee fee : fees) {
            BookingDetailDTO.BookingFeeDTO fd = new BookingDetailDTO.BookingFeeDTO();
            fd.setId(fee.getId());
            fd.setFeeType(fee.getFeeType());
            fd.setAmount(fee.getAmount());
            fd.setDescription(fee.getDescription());
            fd.setIssueId(fee.getIssue() != null ? fee.getIssue().getId() : null);
            fd.setCreatedAt(fee.getCreatedAt());
            feeDtos.add(fd);
        }
        d.setFees(feeDtos);

        return d;
    }

    /** The plate recorded on the booking wins; before that the vehicle's current plate is shown. */
    private String resolvePlate(Booking b) {
        if (b.getLicensePlate() != null) {
            return b.getLicensePlate();
        }
        if (b.getVehicle() == null) {
            return "Unassigned";
        }
        return b.getVehicle().getLicensePlate() != null ? b.getVehicle().getLicensePlate().getPlate() : "N/A";
    }

    private String resolveImageUrl(VehicleType vt) {
        if (vt == null || vt.getImageUrl() == null || vt.getImageUrl().isBlank()) {
            return "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";
        }
        return vt.getImageUrl();
    }
}
