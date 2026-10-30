package com.crms.mvc.service.impl;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final SlotRepository slotRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentService paymentService;
    private final ContractService contractService;
    private final ContractTemplateService contractTemplateService;
    private final BookingFeeService bookingFeeService;
    private final PricingService pricingService;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            SlotRepository slotRepository,
            VehicleTypeRepository vehicleTypeRepository,
            VehicleRepository vehicleRepository,
            EmployeeRepository employeeRepository,
            PromotionRepository promotionRepository,
            PaymentService paymentService,
            ContractService contractService,
            ContractTemplateService contractTemplateService,
            BookingFeeService bookingFeeService,
            PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.slotRepository = slotRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.promotionRepository = promotionRepository;
        this.paymentService = paymentService;
        this.contractService = contractService;
        this.contractTemplateService = contractTemplateService;
        this.bookingFeeService = bookingFeeService;
        this.pricingService = pricingService;
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

        // 1. B2 + B3: Tìm xe thực tế có sẵn thông qua SlotService (loại trừ bảo trì và trùng giờ)
        List<Vehicle> availableVehicles = findAvailableVehiclesInTimeRange(type.getId(), start, end);
        if (availableVehicles.isEmpty()) {
            throw new IllegalStateException("Không còn xe trống thuộc dòng xe này trong khung giờ bạn đã chọn.");
        }

        Vehicle assignedVehicle = null;
        if (dto.getVehicleId() != null) {
            assignedVehicle = availableVehicles.stream()
                    .filter(v -> v.getId().equals(dto.getVehicleId()))
                    .findFirst()
                    .orElse(null);
        }
        if (assignedVehicle == null) {
            assignedVehicle = availableVehicles.get(0);
        }

        // 2. Tạo Slot mới tương ứng
        Slot slot = createSlotForBooking(type.getId(), start, end);

        // 3. Tính toán chi phí thông qua PricingService (B4 + B5)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion appliedPromo = null;

        if (dto.getPromotionCode() != null && !dto.getPromotionCode().trim().isEmpty()) {
            appliedPromo = promotionRepository.findByCodeIgnoreCase(dto.getPromotionCode().trim()).orElse(null);
            if (appliedPromo != null && appliedPromo.getQuantity() != null && appliedPromo.getQuantity() > 0) {
                if ("percent".equalsIgnoreCase(appliedPromo.getDiscountType())) {
                    long roughHours = pricingService.calculateRentalHours(start, end);
                    BigDecimal gross = type.getPricePerHour().multiply(BigDecimal.valueOf(roughHours));
                    BigDecimal rate = appliedPromo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    discountAmount = gross.multiply(rate).setScale(0, RoundingMode.HALF_UP);
                } else {
                    discountAmount = appliedPromo.getDiscountValue();
                }
            }
        }

        PriceQuoteDTO quote = pricingService.calculatePrice(start, end, type.getPricePerHour(), terms.getDepositRate(), discountAmount);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSlot(slot);
        booking.setVehicle(assignedVehicle);
        booking.setPromotion(appliedPromo);
        booking.setPickupTime(start);
        booking.setScheduledEndTime(end);
        booking.setPricePerHour(type.getPricePerHour());
        booking.setDepositAmount(quote.getDepositAmount());
        booking.setDiscountAmount(quote.getDiscountAmount());
        booking.setLateFee(BigDecimal.ZERO);
        booking.setTotalPrice(quote.getTotalPrice());
        booking.setStatus("pending_payment");
        booking.setCreatedAt(OffsetDateTime.now());

        // Tự động gán nhân viên ít đơn nhất để hỗ trợ khách
        List<Employee> staffList = employeeRepository.findLeastLoadedStaff(org.springframework.data.domain.PageRequest.of(0, 1));
        Employee assignedStaff = null;
        if (staffList != null && !staffList.isEmpty()) {
            assignedStaff = staffList.get(0);
        } else {
            List<Employee> allEmployees = employeeRepository.findAll();
            if (!allEmployees.isEmpty()) {
                assignedStaff = allEmployees.get(0);
            }
        }
        booking.setEmployee(assignedStaff);

        Booking saved = bookingRepository.save(booking);

        // Lưu bằng chứng khách đã đồng ý đúng phiên bản điều khoản; PDF được phát hành sau khi cọc
        contractService.createContract(saved, terms.getId(), clientIp);

        return saved;
    }

        @Override
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredPendingBookings() {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(15));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryDTO> getMyBookings(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(b -> {
            BookingSummaryDTO s = new BookingSummaryDTO();
            s.setId(b.getId());
            VehicleType vt = b.getSlot().getVehicleType();
            s.setVehicleTypeName(vt.getName());
            s.setVehicleCategory(vt.getCategory());
            s.setImageUrl(resolveImageUrl(vt));
            s.setLicensePlate(resolvePlate(b));
            s.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
            s.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
            s.setTotalPrice(b.getTotalPrice());
            s.setDepositAmount(b.getDepositAmount());
            s.setStatus(b.getStatus());
            s.setCustomerName(b.getCustomer() != null ? b.getCustomer().getName() : null);
            return s;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailDTO getBookingDetail(Long bookingId, Long customerId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized access to booking");
        }
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailDTO getBookingDetailForAdmin(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryDTO> getAllBookings(String status) {
        List<Booking> list = (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim()))
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
            s.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
            s.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
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

        // B7 + B16: Hoàn cọc 100% nếu hủy trước mốc freeCancellationHours theo hợp đồng khách đã đồng ý
        if ("confirmed".equalsIgnoreCase(b.getStatus())) {
            try {
                ContractTemplateDTO terms = contractService.getTermsOfBooking(bookingId);
                int freeHours = terms.getFreeCancellationHours();
                OffsetDateTime cancelDeadline = b.getPickupTime().minusHours(freeHours);
                if (!OffsetDateTime.now().isAfter(cancelDeadline)) {
                    Payment refund = new Payment();
                    refund.setBooking(b);
                    refund.setPaymentType("REFUND");
                    refund.setAmount(b.getDepositAmount());
                    refund.setPaymentMethod("VNPAY");
                    refund.setTransactionCode("TXN-REF-" + bookingId + "-" + System.currentTimeMillis());
                    refund.setStatus("paid");
                    refund.setPaidAt(OffsetDateTime.now());
                    paymentService.getPaymentsByBooking(bookingId); // touch collection
                }
            } catch (Exception ignored) {
            }
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
        if ("maintenance".equalsIgnoreCase(v.getStatus())) {
            throw new IllegalStateException("Vehicle is currently under maintenance");
        }

        // B3: Chống gán xe trùng giờ với booking khác
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                b.getPickupTime(), b.getScheduledEndTime(), OffsetDateTime.now().minusMinutes(15));
        if (bookedVehicleIds.contains(vehicleId) && (b.getVehicle() == null || !b.getVehicle().getId().equals(vehicleId))) {
            throw new IllegalStateException("Xe này đã có lịch đặt khác trong cùng khung giờ.");
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

        // B4: Tính phí trễ qua PricingService có ân hạn 15 phút và làm tròn lên
        BigDecimal multiplier = contractService.getTermsOfBooking(bookingId).getLateFeeMultiplier();
        BigDecimal lateFee = pricingService.calculateLateFee(b.getScheduledEndTime(), actualReturn, b.getPricePerHour(), multiplier);

        paymentService.processSettlement(bookingId, lateFee, dto != null ? dto.getAdditionalFine() : BigDecimal.ZERO);

        if (b.getVehicle() != null) {
            boolean hasDamage = dto != null && dto.getReturnNotes() != null
                    && (dto.getReturnNotes().toLowerCase().contains("damage")
                    || dto.getReturnNotes().toLowerCase().contains("hỏng")
                    || dto.getReturnNotes().toLowerCase().contains("sự cố"));
            b.getVehicle().setStatus(hasDamage ? "maintenance" : "available");
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
        d.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
        d.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
        d.setActualReturnTime(DateTimeUtils.toVietnamTime(b.getActualReturnTime()));

        d.setPricePerHour(b.getPricePerHour());
        d.setDepositAmount(b.getDepositAmount());
        d.setDiscountAmount(b.getDiscountAmount());
        d.setLateFee(b.getLateFee());
        d.setTotalPrice(b.getTotalPrice());
        d.setPromotionCode(b.getPromotion() != null ? b.getPromotion().getCode() : null);

        d.setStatus(b.getStatus());
        d.setCreatedAt(DateTimeUtils.toVietnamTime(b.getCreatedAt()));

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
            pd.setPaidAt(DateTimeUtils.toVietnamTime(p.getPaidAt()));
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

        if (b.getEmployee() != null) {
            d.setStaffId(b.getEmployee().getId());
            d.setStaffName(b.getEmployee().getName());
            d.setStaffPhone(b.getEmployee().getPhone());
            d.setStaffEmail(b.getEmployee().getEmail());
            d.setStationAddress("Kho xe DriveNow - 268 Lý Thường Kiệt, P.14, Q.10, TP.HCM");
        } else {
            d.setStaffName("Bãi xe DriveNow (Quản lý)");
            d.setStaffPhone("1900 6868");
            d.setStationAddress("Kho xe DriveNow - 268 Lý Thường Kiệt, P.14, Q.10, TP.HCM");
        }

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

    @Override
    @Transactional(readOnly = true)
    public long countTotalBookings() {
        return bookingRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Booking> findEntityById(Long id) {
        return bookingRepository.findById(id);
    }

    private String resolveImageUrl(VehicleType vt) {
        if (vt == null || vt.getImageUrl() == null || vt.getImageUrl().isBlank()) {
            return "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";
        }
        return vt.getImageUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Vehicle> allVehiclesOfType = vehicleRepository.findByVehicleTypeIdWithDetails(vehicleTypeId);

        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(15);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(startTime, endTime, cutoffTime);

        return allVehiclesOfType.stream()
                .filter(v -> !"maintenance".equalsIgnoreCase(v.getStatus()))
                .filter(v -> !bookedVehicleIds.contains(v.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category) {
        List<VehicleType> allTypes = vehicleTypeRepository.findAll();
        List<VehicleTypeSearchResultDTO> results = new ArrayList<>();

        BigDecimal depositRate = contractTemplateService.getActiveTemplate().getDepositRate();

        for (VehicleType vt : allTypes) {
            if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
                if (!category.equalsIgnoreCase(vt.getCategory())) {
                    continue;
                }
            }

            List<Vehicle> availableCars = findAvailableVehiclesInTimeRange(vt.getId(), startTime, endTime);

            VehicleTypeSearchResultDTO dto = new VehicleTypeSearchResultDTO();
            dto.setId(vt.getId());
            dto.setName(vt.getName());
            dto.setBrand(vt.getBrand());
            dto.setCategory(vt.getCategory());
            dto.setSeats(vt.getSeats());
            dto.setTransmission(vt.getTransmission());
            dto.setFuelType(vt.getFuelType());
            dto.setPricePerHour(vt.getPricePerHour());

            PriceQuoteDTO quote = pricingService.calculatePrice(startTime, endTime, vt.getPricePerHour(), depositRate, BigDecimal.ZERO);
            dto.setEstimatedTotal(quote.getTotalPrice());
            dto.setDepositAmount(quote.getDepositAmount());

            String img = vt.getImageUrl();
            if (img == null || img.isBlank()) {
                img = "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";
            }
            dto.setImageUrl(img);
            dto.setRating(4.8);
            dto.setReviewCount(120);
            dto.setAvailableCount(availableCars.size());

            results.add(dto);
        }

        return results;
    }

    @Override
    public Slot createSlotForBooking(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        VehicleType type = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new NoSuchElementException("VehicleType not found: " + vehicleTypeId));

        Slot slot = new Slot();
        slot.setVehicleType(type);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(1);
        slot.setStatus("open");

        return slotRepository.save(slot);
    }

}
