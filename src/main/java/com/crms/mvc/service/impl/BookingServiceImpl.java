package com.crms.mvc.service.impl;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.CompensationVoucherService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    public static final int BUFFER_BETWEEN_RENTALS_HOURS = 1;

    @org.springframework.beans.factory.annotation.Value("${crms.opening-hours.open:0}")
    private int openingHour = com.crms.mvc.config.OpeningHours.DEFAULT.open();
    @org.springframework.beans.factory.annotation.Value("${crms.opening-hours.close:23}")
    private int closingHour = com.crms.mvc.config.OpeningHours.DEFAULT.close();
    @org.springframework.beans.factory.annotation.Value("${crms.station.address:Kho xe DriveNow - 268 Lý Thường Kiệt, P.14, Q.10, TP.HCM}")
    private String stationAddress = "Kho xe DriveNow - 268 Lý Thường Kiệt, P.14, Q.10, TP.HCM";

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
    private final CompensationVoucherService compensationVoucherService;

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
            PricingService pricingService,
            CompensationVoucherService compensationVoucherService) {
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
        this.compensationVoucherService = compensationVoucherService;
    }

    @Override
    public void validateRentalSchedule(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian nhận xe.");
        }
        if (end == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian trả xe.");
        }
        if (!isWholeHour(start)) {
            throw new IllegalArgumentException("Thời gian nhận xe phải là giờ chẵn, ví dụ 09:00.");
        }
        if (!isWholeHour(end)) {
            throw new IllegalArgumentException("Thời gian trả xe phải là giờ chẵn, ví dụ 18:00.");
        }
        com.crms.mvc.config.OpeningHours hours = new com.crms.mvc.config.OpeningHours(openingHour, closingHour);
        if (!hours.contains(start)) {
            throw new IllegalArgumentException("Giờ nhận xe phải trong giờ mở cửa của kho (" + hours.getLabel() + ").");
        }
        if (!hours.contains(end)) {
            throw new IllegalArgumentException("Giờ trả xe phải trong giờ mở cửa của kho (" + hours.getLabel() + ").");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian nhận xe không được ở trong quá khứ.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian trả xe phải sau thời gian nhận xe.");
        }
    }

    /** Rentals are booked by the whole hour only: minutes, seconds and nanoseconds must all be zero. */
    private static boolean isWholeHour(LocalDateTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    @Override
    public Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + customerId));

        validateRentalSchedule(dto.getStartTime(), dto.getEndTime());

        // A customer who gave a car back but has not settled it yet must settle before renting again.
        List<Booking> unsettled = bookingRepository.findReturnedUnsettledOfCustomer(customerId);
        if (!unsettled.isEmpty()) {
            throw new IllegalStateException("Bạn còn đơn #" + unsettled.get(0).getId()
                    + " đã trả xe nhưng chưa quyết toán. Hãy quyết toán đơn này trước khi đặt đơn mới.");
        }

        ZoneOffset defaultOffset = ZoneOffset.ofHours(7); // UTC+7 for Vietnam
        OffsetDateTime start = dto.getStartTime().atOffset(defaultOffset);
        OffsetDateTime end = dto.getEndTime().atOffset(defaultOffset);

        VehicleType type = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new NoSuchElementException("VehicleType not found: " + dto.getVehicleTypeId()));

        // Clickwrap: the customer is bound only by the version they saw on the checkout page.
        ContractTemplateDTO terms = contractTemplateService.getActiveTemplate();
        if (!terms.getId().equals(dto.getContractTemplateId())) {
            throw new IllegalStateException("Điều khoản thuê xe vừa được cập nhật trong lúc bạn đặt xe. "
                    + "Vui lòng xem và đồng ý phiên bản mới.");
        }

        // 0. Tự động hủy các đơn pending quá hạn thanh toán (Payment.PAYMENT_TIMEOUT_MINUTES) của toàn hệ thống
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES));

        // One person can only drive one car at a time (the contract forbids handing the car to another driver).
        List<Booking> overlapping = bookingRepository.findPaidBookingsByCustomerAndTimeRange(customerId, start, end);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Bạn đã có đơn #" + overlapping.get(0).getId()
                    + " trong khung giờ này. Mỗi khách chỉ thuê một xe tại một thời điểm.");
        }

        // 0.1 Nếu khách hàng này đã có đơn 'pending_payment' trong cùng khung giờ,
        // hệ thống tự hủy đơn pending cũ đó để nhường chỗ cho lần tạo mới này!
        List<Booking> existingPendingBookings = bookingRepository.findPendingBookingsByCustomerAndTimeRange(customerId,
                start, end);
        for (Booking oldPending : existingPendingBookings) {
            oldPending.setStatus("cancelled");
            oldPending.setCancelledAt(OffsetDateTime.now());
            bookingRepository.save(oldPending);
        }

        // Serialize bookings of the same vehicle type so two customers cannot both get the last free car.
        vehicleRepository.lockByVehicleTypeId(type.getId());

        // 1. B2 + B3: Tìm xe thực tế có sẵn thông qua SlotService (loại trừ bảo trì và trùng giờ)
        List<Vehicle> availableVehicles = findAvailableVehiclesInTimeRange(type.getId(), start, end);
        if (availableVehicles.isEmpty()) {
            throw new IllegalStateException("Không còn xe trống thuộc dòng xe này trong khung giờ bạn đã chọn.");
        }

        // The plate the customer picked, or any free car of the type when they picked none.
        // Never swap a picked plate silently: it may have been taken while they were on the checkout page.
        Vehicle assignedVehicle = dto.getVehicleId() == null
                ? availableVehicles.get(0)
                : availableVehicles.stream()
                        .filter(v -> v.getId().equals(dto.getVehicleId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Xe bạn chọn vừa được đặt trong khung giờ này. Vui lòng chọn xe khác hoặc để hệ thống tự chọn."));

        // 2. Tạo Slot mới tương ứng
        Slot slot = createSlotForBooking(type.getId(), start, end);

        // 3. Tính toán chi phí thông qua PricingService (B4 + B5)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion appliedPromo = null;

        if (dto.getPromotionCode() != null && !dto.getPromotionCode().trim().isEmpty()) {
            appliedPromo = promotionRepository.findByCodeForUpdate(dto.getPromotionCode().trim())
                    .filter(BookingServiceImpl::isUsableToday)
                    .orElse(null);
            if (appliedPromo != null) {
                appliedPromo.setQuantity(appliedPromo.getQuantity() - 1);
                promotionRepository.save(appliedPromo);
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
        booking.setTotalPrice(quote.getTotalPrice());
        booking.setStatus("pending_payment");
        booking.setCreatedAt(OffsetDateTime.now());

        // Tự động gán nhân viên ít đơn nhất để hỗ trợ khách
        List<Employee> staffList = employeeRepository.findLeastLoadedStaff(org.springframework.data.domain.PageRequest.of(0, 1));
        Employee assignedStaff = null;
        if (staffList != null && !staffList.isEmpty()) {
            assignedStaff = staffList.get(0);
        } else {
            List<Employee> allEmployees = employeeRepository.findByActiveTrueOrderByNameAsc();
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
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES));
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
            s.setOverdueMinutes(overdueMinutes(b, OffsetDateTime.now()));
            s.setPickupDeadline(awaitingPickupUntil(b, OffsetDateTime.now()));
            return s;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailDTO getBookingDetail(Long bookingId, Long customerId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Bạn không có quyền truy cập đơn đặt xe này");
        }
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailDTO getBookingDetailForAdmin(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
        return mapToDetailDTO(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryDTO> getAllBookings(String status) {
        List<Long> pendingRefundBookingIds = paymentService.getPendingRefundBookingIds();
        Set<Long> pendingRefundSet = new HashSet<>(pendingRefundBookingIds);

        List<Booking> list;
        if ("refund".equalsIgnoreCase(status) || "refunds".equalsIgnoreCase(status) || "pending_refund".equalsIgnoreCase(status)) {
            list = bookingRepository.findBookingsWithPendingRefund();
        } else if (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim())) {
            list = bookingRepository.findAllByOrderByCreatedAtDesc();
        } else {
            list = bookingRepository.findByStatusOrderByCreatedAtDesc(status.trim());
        }

        return list.stream().map(b -> {
            BookingSummaryDTO s = new BookingSummaryDTO();
            s.setId(b.getId());
            VehicleType vt = (b.getSlot() != null) ? b.getSlot().getVehicleType() : null;
            if (vt != null) {
                s.setVehicleTypeName(vt.getName());
                s.setVehicleCategory(vt.getCategory());
                s.setImageUrl(resolveImageUrl(vt));
            } else {
                s.setVehicleTypeName("Phương tiện DriveNow");
            }
            s.setLicensePlate(resolvePlate(b));
            s.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
            s.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
            s.setTotalPrice(b.getTotalPrice());
            s.setDepositAmount(b.getDepositAmount());
            boolean isPendingRefund = pendingRefundSet.contains(b.getId());
            s.setStatus(isPendingRefund ? "pending_refund" : b.getStatus());
            s.setCustomerName(b.getCustomer() != null ? b.getCustomer().getName() : null);
            s.setOverdueMinutes(overdueMinutes(b, OffsetDateTime.now()));
            s.setPickupDeadline(awaitingPickupUntil(b, OffsetDateTime.now()));
            if (isPendingRefund) {
                s.setHasPendingRefund(true);
                s.setRefundAmount(b.getDepositAmount());
            }
            return s;
        }).toList();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long customerId) {
        cancelBooking(bookingId, customerId, null);
    }

    @Override
    @Transactional
    public boolean cancelBooking(Long bookingId, Long customerId, BookingCancelDTO dto) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized action");
        }
        if (!"pending_payment".equalsIgnoreCase(b.getStatus()) && !"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Không thể hủy đơn ở trạng thái: " + b.getStatus());
        }

        boolean refundEligible = false;
        // B7 + B16: Hoàn cọc 100% nếu hủy trước mốc freeCancellationHours theo hợp đồng khách đã đồng ý
        if ("confirmed".equalsIgnoreCase(b.getStatus())) {
            OffsetDateTime depositPaidAt = paymentService.getPaymentsByBooking(bookingId).stream()
                    .filter(p -> Payment.TYPE_DEPOSIT.equalsIgnoreCase(p.getPaymentType())
                            && Payment.STATUS_PAID.equalsIgnoreCase(p.getStatus()))
                    .map(Payment::getPaidAt)
                    .filter(java.util.Objects::nonNull)
                    .findFirst().orElse(null);
            OffsetDateTime cancelDeadline = contractService.getTermsOfBooking(bookingId)
                    .freeCancellationUntil(b.getPickupTime(), depositPaidAt);
            if (!OffsetDateTime.now().isAfter(cancelDeadline)) {
                refundDeposit(b, dto);
                refundEligible = true;
            }
        }

        b.setStatus("cancelled");
        b.setCancelledAt(OffsetDateTime.now());
        bookingRepository.save(b);
        return refundEligible;
    }

    @Override
    @Transactional
    public void cancelForUnavailableCar(Long bookingId, String refundMethod) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ huỷ theo cách này với đơn đã đặt cọc và chưa bàn giao (trạng thái đơn: " + b.getStatus() + ")");
        }
        // Only when the company really has no car of the booked type left: otherwise staff swap the car instead,
        // and a customer who no longer wants it cancels under the normal rules.
        if (!findAvailableVehiclesInTimeRange(b.getSlot().getVehicleType().getId(), b.getPickupTime(), b.getScheduledEndTime()).isEmpty()) {
            throw new IllegalStateException("Vẫn còn xe cùng loại trống trong khung giờ thuê: hãy đổi xe cho khách thay vì huỷ đơn.");
        }
        // The contract cannot be fulfilled: the whole deposit goes back at the counter, whatever the cancellation deadline.
        paymentService.recordCounterRefund(bookingId, b.getDepositAmount(), refundMethod);
        b.setStatus("cancelled");
        b.setCancelledAt(OffsetDateTime.now());
        bookingRepository.save(b);
        compensationVoucherService.issueFor(b);
    }

    /** 10.00 prints as "10": stripTrailingZeros alone turns 10 into 1E+1, which pages would show as "1E+1%". */
    private static BigDecimal plainPercent(BigDecimal percent) {
        BigDecimal stripped = percent.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /** A promotion code applies only between its start and end dates (Vietnam time) and while it has uses left. */
    private static boolean isUsableToday(Promotion promotion) {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        return promotion.getQuantity() != null && promotion.getQuantity() > 0
                && (promotion.getStartDate() == null || !today.isBefore(promotion.getStartDate()))
                && (promotion.getEndDate() == null || !today.isAfter(promotion.getEndDate()));
    }

    private void refundDeposit(Booking b, BookingCancelDTO dto) {
        String bank = (dto != null && dto.getBankCode() != null && !dto.getBankCode().isBlank()) ? dto.getBankCode().trim() : "BANK";
        String acc = (dto != null && dto.getAccountNumber() != null && !dto.getAccountNumber().isBlank()) ? dto.getAccountNumber().trim() : "";
        String holder = (dto != null && dto.getAccountHolder() != null && !dto.getAccountHolder().isBlank()) ? dto.getAccountHolder().trim().toUpperCase() : "";
        paymentService.createPendingRefund(b.getId(), b.getDepositAmount(), bank, acc, holder);
    }

    @Override
    public void assignVehicle(Long bookingId, Long vehicleId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
        // A car can only be swapped before handover: afterwards the customer is driving the assigned one.
        if (!"pending_payment".equalsIgnoreCase(b.getStatus()) && !"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ đổi được xe trước khi bàn giao (trạng thái đơn: " + b.getStatus() + ")");
        }
        Vehicle v = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy xe: " + vehicleId));

        if (!v.getVehicleType().getId().equals(b.getSlot().getVehicleType().getId())) {
            throw new IllegalStateException("Chỉ đổi được sang xe cùng loại với loại xe khách đã đặt.");
        }
        if ("maintenance".equalsIgnoreCase(v.getStatus()) || Vehicle.STATUS_RETIRED.equalsIgnoreCase(v.getStatus())) {
            throw new IllegalStateException("Xe này không thể gán cho đơn thuê");
        }

        // B3: Chống gán xe trùng giờ với booking khác (có buffer 1 tiếng)
        OffsetDateTime bufferedStart = b.getPickupTime().minusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        OffsetDateTime bufferedEnd = b.getScheduledEndTime().plusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                bufferedStart, bufferedEnd, OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES), OffsetDateTime.now());
        if (bookedVehicleIds.contains(vehicleId) && (b.getVehicle() == null || !b.getVehicle().getId().equals(vehicleId))) {
            throw new IllegalStateException("Xe này đã có lịch đặt khác trong cùng khung giờ (yêu cầu cách nhau tối thiểu 1 tiếng).");
        }

        b.setVehicle(v);
        bookingRepository.save(b);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdueRentals() {
        return bookingRepository.countOverdueRentals(OffsetDateTime.now());
    }

    /** While a paid booking waits for its customer after the pickup time: when it will be cancelled; else null. */
    private OffsetDateTime awaitingPickupUntil(Booking b, OffsetDateTime now) {
        if (!"confirmed".equalsIgnoreCase(b.getStatus()) || b.getPickupTime() == null || now.isBefore(b.getPickupTime())) {
            return null;
        }
        return awaitingPickupUntil(b, contractService.getTermsOfBooking(b.getId()), now);
    }

    private static OffsetDateTime awaitingPickupUntil(Booking b, ContractTemplateDTO terms, OffsetDateTime now) {
        if (!"confirmed".equalsIgnoreCase(b.getStatus()) || b.getPickupTime() == null || now.isBefore(b.getPickupTime())) {
            return null;
        }
        return DateTimeUtils.toVietnamTime(terms.pickupDeadline(b.getPickupTime(), b.getScheduledEndTime()));
    }

    /** An active rental whose car has not come back although its scheduled return time has passed. */
    private static boolean isOverdue(Booking b, OffsetDateTime now) {
        return "active".equalsIgnoreCase(b.getStatus()) && b.getActualReturnTime() == null
                && b.getScheduledEndTime() != null && now.isAfter(b.getScheduledEndTime());
    }

    private static Long overdueMinutes(Booking b, OffsetDateTime now) {
        return isOverdue(b, now) ? java.time.Duration.between(b.getScheduledEndTime(), now).toMinutes() : null;
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
            d.setVehicleRetired(Vehicle.STATUS_RETIRED.equalsIgnoreCase(b.getVehicle().getStatus()));
            d.setVehicleStatus(b.getVehicle().getStatus());
            d.setVehicleOdometer(b.getVehicle().getOdometer());
        }
        d.setLicensePlate(resolvePlate(b));

        d.setSlotId(b.getSlot().getId());
        d.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
        d.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
        d.setActualPickupTime(DateTimeUtils.toVietnamTime(b.getActualPickupTime()));
        d.setActualReturnTime(DateTimeUtils.toVietnamTime(b.getActualReturnTime()));

        d.setPricePerHour(b.getPricePerHour());
        d.setDepositAmount(b.getDepositAmount());
        d.setDiscountAmount(b.getDiscountAmount());
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
        if (d.getContract() != null && d.getContract().getTerms() != null) {
            d.setPickupDeadline(awaitingPickupUntil(b, d.getContract().getTerms(), OffsetDateTime.now()));
        }
        if ("cancelled".equalsIgnoreCase(b.getStatus())) {
            compensationVoucherService.findForBooking(b.getId()).ifPresent(v -> {
                d.setCompensationVoucherCode(v.getCode());
                d.setCompensationVoucherValidUntil(v.getEndDate());
                d.setCompensationVoucherUsed(v.getQuantity() == null || v.getQuantity() <= 0);
                d.setCompensationVoucherPercent(v.getDiscountValue() != null ? plainPercent(v.getDiscountValue()) : null);
            });
        }
        if (isOverdue(b, OffsetDateTime.now())) {
            BigDecimal multiplier = d.getContract() != null && d.getContract().getTerms() != null
                    ? d.getContract().getTerms().getLateFeeMultiplier() : null;
            BigDecimal rate = b.getPricePerHour() != null ? b.getPricePerHour() : vt.getPricePerHour();
            d.setLateReturn(pricingService.describeLateReturn(b.getScheduledEndTime(), OffsetDateTime.now(), rate, multiplier));
        }

        List<BookingFee> fees = bookingFeeService.getFeesByBooking(b.getId());
        List<BookingDetailDTO.BookingFeeDTO> feeDtos = new ArrayList<>();
        BigDecimal lateFeeTotal = BigDecimal.ZERO;
        for (BookingFee fee : fees) {
            BookingDetailDTO.BookingFeeDTO fd = new BookingDetailDTO.BookingFeeDTO();
            fd.setId(fee.getId());
            fd.setFeeType(fee.getFeeType());
            fd.setAmount(fee.getAmount());
            fd.setDescription(fee.getDescription());
            fd.setIssueId(fee.getIssue() != null ? fee.getIssue().getId() : null);
            fd.setCreatedAt(fee.getCreatedAt());
            feeDtos.add(fd);
            if ("LATE_FEE".equalsIgnoreCase(fee.getFeeType())) {
                lateFeeTotal = lateFeeTotal.add(fee.getAmount());
            }
        }
        d.setFees(feeDtos);
        d.setLateFee(lateFeeTotal);

        if (b.getEmployee() != null) {
            d.setStaffId(b.getEmployee().getId());
            d.setStaffName(b.getEmployee().getName());
            d.setStaffPhone(b.getEmployee().getPhone());
            d.setStaffEmail(b.getEmployee().getEmail());
            d.setStationAddress(stationAddress);
        } else {
            d.setStaffName("Bãi xe DriveNow (Quản lý)");
            d.setStaffPhone("1900 6868");
            d.setStationAddress(stationAddress);
        }

        return d;
    }

    /** Lấy biển số trực tiếp từ xe vật lý được gán cho đơn đặt xe */
    private String resolvePlate(Booking b) {
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

        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES);
        OffsetDateTime bufferedStart = startTime.minusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        OffsetDateTime bufferedEnd = endTime.plusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                bufferedStart, bufferedEnd, cutoffTime, OffsetDateTime.now());

        java.time.ZoneOffset vietnam = java.time.ZoneOffset.ofHours(7);
        java.time.LocalDate startDay = startTime.withOffsetSameInstant(vietnam).toLocalDate();
        // The last moment the car is driven is just before the end: a rental ending at 00:00 ends on the day before.
        java.time.LocalDate endDay = endTime.minusNanos(1).withOffsetSameInstant(vietnam).toLocalDate();
        java.util.Map<Long, List<java.time.LocalDate>> inspectionDays = new java.util.HashMap<>();
        for (com.crms.mvc.entity.VehicleInspection inspection : vehicleRepository.findScheduledInspectionsByVehicleTypeId(vehicleTypeId)) {
            inspectionDays.computeIfAbsent(inspection.getVehicle().getId(), id -> new ArrayList<>())
                    .add(inspection.getScheduledAt().withOffsetSameInstant(vietnam).toLocalDate());
        }

        return allVehiclesOfType.stream()
                .filter(v -> !"maintenance".equalsIgnoreCase(v.getStatus()) && !Vehicle.STATUS_RETIRED.equalsIgnoreCase(v.getStatus()))
                .filter(v -> !bookedVehicleIds.contains(v.getId()))
                .filter(v -> !com.crms.mvc.service.InspectionRules.blocksRental(
                        v.getRegistrationExpiry(), inspectionDays.getOrDefault(v.getId(), List.of()), startDay, endDay))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleScheduleDTO getVehicleSchedule(Long vehicleId, LocalDate fromDate, int days) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy xe: " + vehicleId));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = fromDate.atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        OffsetDateTime to = from.plusDays(days);
        List<OffsetDateTime[]> held = bookingRepository.findBookingsHoldingVehicle(vehicleId,
                        from.minusHours(BUFFER_BETWEEN_RENTALS_HOURS), to.plusHours(BUFFER_BETWEEN_RENTALS_HOURS),
                        now.minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES))
                .stream()
                .map(b -> new OffsetDateTime[]{b.getPickupTime(), heldUntil(b, now)})
                .toList();

        // Days the customer cannot book for inspection reasons are marked too, so a free-looking hour is really bookable.
        List<LocalDate> inspectionDays = vehicleRepository.findScheduledInspectionsByVehicleId(vehicleId).stream()
                .map(i -> i.getScheduledAt().withOffsetSameInstant(ZoneOffset.ofHours(7)).toLocalDate())
                .toList();

        List<VehicleScheduleDTO.Day> scheduleDays = new ArrayList<>();
        for (int day = 0; day < days; day++) {
            LocalDate date = fromDate.plusDays(day);
            com.crms.mvc.service.InspectionRules.DayBlock block = com.crms.mvc.service.InspectionRules.blockOn(
                    vehicle.getRegistrationExpiry(), inspectionDays, date);
            List<VehicleScheduleDTO.HourState> hours = new ArrayList<>(24);
            for (int hour = 0; hour < 24; hour++) {
                OffsetDateTime hourStart = from.plusDays(day).plusHours(hour);
                VehicleScheduleDTO.HourState state = hourState(hourStart, hourStart.plusHours(1), held, now);
                if (state == VehicleScheduleDTO.HourState.FREE) {
                    state = switch (block) {
                        case INSPECTION_DAY -> VehicleScheduleDTO.HourState.INSPECTION;
                        case EXPIRED -> VehicleScheduleDTO.HourState.EXPIRED;
                        case NONE -> state;
                    };
                }
                hours.add(state);
            }
            scheduleDays.add(new VehicleScheduleDTO.Day(date, hours));
        }

        VehicleScheduleDTO schedule = new VehicleScheduleDTO();
        schedule.setVehicleId(vehicle.getId());
        schedule.setLicensePlate(vehicle.getLicensePlate() != null ? vehicle.getLicensePlate().getPlate() : null);
        schedule.setVehicleTypeId(vehicle.getVehicleType().getId());
        schedule.setVehicleTypeName(vehicle.getVehicleType().getDisplayName());
        schedule.setVehicleStatus(vehicle.getStatus());
        schedule.setDays(scheduleDays);
        return schedule;
    }

    /** A car out on a rental that has not come back stays held until it does, even past its scheduled end. */
    private static OffsetDateTime heldUntil(Booking booking, OffsetDateTime now) {
        boolean notBackYet = "active".equalsIgnoreCase(booking.getStatus()) && booking.getActualReturnTime() == null;
        return notBackYet && now.isAfter(booking.getScheduledEndTime()) ? now : booking.getScheduledEndTime();
    }

    private static VehicleScheduleDTO.HourState hourState(OffsetDateTime start, OffsetDateTime end,
                                                          List<OffsetDateTime[]> held, OffsetDateTime now) {
        if (!end.isAfter(now)) {
            return VehicleScheduleDTO.HourState.PAST;
        }
        if (held.stream().anyMatch(h -> overlaps(start, end, h[0], h[1]))) {
            return VehicleScheduleDTO.HourState.BOOKED;
        }
        // The hour we are in has already begun, and a rental starts on a whole hour: the next hour is the earliest.
        if (start.isBefore(now)) {
            return VehicleScheduleDTO.HourState.PAST;
        }
        boolean inBuffer = held.stream().anyMatch(h ->
                overlaps(start, end, h[0].minusHours(BUFFER_BETWEEN_RENTALS_HOURS), h[0])
                        || overlaps(start, end, h[1], h[1].plusHours(BUFFER_BETWEEN_RENTALS_HOURS)));
        return inBuffer ? VehicleScheduleDTO.HourState.BUFFER : VehicleScheduleDTO.HourState.FREE;
    }

    private static boolean overlaps(OffsetDateTime start, OffsetDateTime end, OffsetDateTime otherStart, OffsetDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
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
