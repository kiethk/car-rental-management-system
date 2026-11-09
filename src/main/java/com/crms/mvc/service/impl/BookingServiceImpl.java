package com.crms.mvc.service.impl;

import com.crms.mvc.config.OpeningHours;
import com.crms.mvc.dto.BookingCancelDTO;
import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.service.VehicleAvailabilityService;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/** The life of a booking: create, cancel (with or without a refund), and change the car before handover. */
@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    @Value("${crms.opening-hours.open:0}")
    private int openingHour = OpeningHours.DEFAULT.open();
    @Value("${crms.opening-hours.close:23}")
    private int closingHour = OpeningHours.DEFAULT.close();

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
    private final PricingService pricingService;
    private final VehicleAvailabilityService availability;

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
            PricingService pricingService,
            VehicleAvailabilityService availability) {
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
        this.pricingService = pricingService;
        this.availability = availability;
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
        OpeningHours hours = new OpeningHours(openingHour, closingHour);
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

    // ---- create ----

    @Override
    public Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + customerId));

        validateRentalSchedule(dto.getStartTime(), dto.getEndTime());
        requireNoUnsettledRental(customerId);

        OffsetDateTime start = dto.getStartTime().atOffset(DateTimeUtils.VIETNAM_OFFSET);
        OffsetDateTime end = dto.getEndTime().atOffset(DateTimeUtils.VIETNAM_OFFSET);
        VehicleType type = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new NoSuchElementException("VehicleType not found: " + dto.getVehicleTypeId()));
        ContractTemplateDTO terms = requireTermsTheCustomerSaw(dto);

        makeRoomFor(customerId, start, end);

        // Serialize bookings of the same vehicle type so two customers cannot both get the last free car.
        vehicleRepository.lockByVehicleTypeId(type.getId());
        Vehicle car = pickVehicle(type, dto.getVehicleId(), start, end);
        Slot slot = openSlot(type, start, end);

        Promotion promotion = redeemPromotion(dto.getPromotionCode());
        BigDecimal discount = discountOf(promotion, type, start, end);
        PriceQuoteDTO quote = pricingService.calculatePrice(start, end, type.getPricePerHour(), terms.getDepositRate(), discount);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSlot(slot);
        booking.setVehicle(car);
        booking.setPromotion(promotion);
        booking.setPickupTime(start);
        booking.setScheduledEndTime(end);
        booking.setPricePerHour(type.getPricePerHour());
        booking.setDepositAmount(quote.getDepositAmount());
        booking.setDiscountAmount(quote.getDiscountAmount());
        booking.setTotalPrice(quote.getTotalPrice());
        booking.setStatus(Booking.STATUS_PENDING_PAYMENT);
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setEmployee(leastLoadedStaff());

        Booking saved = bookingRepository.save(booking);

        // Lưu bằng chứng khách đã đồng ý đúng phiên bản điều khoản; PDF được phát hành sau khi cọc
        contractService.createContract(saved, terms.getId(), clientIp);
        return saved;
    }

    /** A customer who gave a car back but has not settled it yet must settle before renting again. */
    private void requireNoUnsettledRental(Long customerId) {
        List<Booking> unsettled = bookingRepository.findReturnedUnsettledOfCustomer(customerId);
        if (!unsettled.isEmpty()) {
            throw new IllegalStateException("Bạn còn đơn #" + unsettled.get(0).getId()
                    + " đã trả xe nhưng chưa quyết toán. Hãy quyết toán đơn này trước khi đặt đơn mới.");
        }
    }

    /** Clickwrap: the customer is bound only by the version they saw on the checkout page. */
    private ContractTemplateDTO requireTermsTheCustomerSaw(BookingCreateDTO dto) {
        ContractTemplateDTO terms = contractTemplateService.getActiveTemplate();
        if (!terms.getId().equals(dto.getContractTemplateId())) {
            throw new IllegalStateException("Điều khoản thuê xe vừa được cập nhật trong lúc bạn đặt xe. "
                    + "Vui lòng xem và đồng ý phiên bản mới.");
        }
        return terms;
    }

    /**
     * Frees what would be in the new booking's way: expired unpaid bookings anywhere are cancelled, a paid booking of
     * this customer in the same hours refuses the new one (one person drives one car at a time), and an unpaid one is
     * cancelled to make room for it.
     */
    private void makeRoomFor(Long customerId, OffsetDateTime start, OffsetDateTime end) {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES));

        List<Booking> overlapping = bookingRepository.findPaidBookingsByCustomerAndTimeRange(customerId, start, end);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Bạn đã có đơn #" + overlapping.get(0).getId()
                    + " trong khung giờ này. Mỗi khách chỉ thuê một xe tại một thời điểm.");
        }

        for (Booking oldPending : bookingRepository.findPendingBookingsByCustomerAndTimeRange(customerId, start, end)) {
            oldPending.setStatus(Booking.STATUS_CANCELLED);
            oldPending.setCancelledAt(OffsetDateTime.now());
            bookingRepository.save(oldPending);
        }
    }

    /**
     * The plate the customer picked, or any free car of the type when they picked none. A picked plate is never
     * swapped silently: it may have been taken while they were on the checkout page.
     */
    private Vehicle pickVehicle(VehicleType type, Long requestedVehicleId, OffsetDateTime start, OffsetDateTime end) {
        List<Vehicle> free = availability.findAvailableVehiclesInTimeRange(type.getId(), start, end);
        if (free.isEmpty()) {
            throw new IllegalStateException("Không còn xe trống thuộc dòng xe này trong khung giờ bạn đã chọn.");
        }
        if (requestedVehicleId == null) {
            return free.get(0);
        }
        return free.stream()
                .filter(v -> v.getId().equals(requestedVehicleId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Xe bạn chọn vừa được đặt trong khung giờ này. Vui lòng chọn xe khác hoặc để hệ thống tự chọn."));
    }

    private Slot openSlot(VehicleType type, OffsetDateTime start, OffsetDateTime end) {
        Slot slot = new Slot();
        slot.setVehicleType(type);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setCapacity(1);
        slot.setStatus("open");
        return slotRepository.save(slot);
    }

    /** Uses one of the code's remaining uses, or returns null when there is no code or it cannot be used today. */
    private Promotion redeemPromotion(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        Promotion promotion = promotionRepository.findByCodeForUpdate(code.trim())
                .filter(BookingServiceImpl::isUsableToday)
                .orElse(null);
        if (promotion != null) {
            promotion.setQuantity(promotion.getQuantity() - 1);
            promotionRepository.save(promotion);
        }
        return promotion;
    }

    private BigDecimal discountOf(Promotion promotion, VehicleType type, OffsetDateTime start, OffsetDateTime end) {
        if (promotion == null) {
            return BigDecimal.ZERO;
        }
        if (!"percent".equalsIgnoreCase(promotion.getDiscountType())) {
            return promotion.getDiscountValue();
        }
        long roughHours = pricingService.calculateRentalHours(start, end);
        BigDecimal gross = type.getPricePerHour().multiply(BigDecimal.valueOf(roughHours));
        BigDecimal rate = promotion.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return gross.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    /** A promotion code applies only between its start and end dates (Vietnam time) and while it has uses left. */
    private static boolean isUsableToday(Promotion promotion) {
        LocalDate today = LocalDate.now(DateTimeUtils.VIETNAM_ZONE);
        return promotion.getQuantity() != null && promotion.getQuantity() > 0
                && (promotion.getStartDate() == null || !today.isBefore(promotion.getStartDate()))
                && (promotion.getEndDate() == null || !today.isAfter(promotion.getEndDate()));
    }

    /** The staff member with the fewest bookings, else the first active one, so the customer has someone to ask. */
    private Employee leastLoadedStaff() {
        List<Employee> staffList = employeeRepository.findLeastLoadedStaff(PageRequest.of(0, 1));
        if (staffList != null && !staffList.isEmpty()) {
            return staffList.get(0);
        }
        List<Employee> allEmployees = employeeRepository.findByActiveTrueOrderByNameAsc();
        return allEmployees.isEmpty() ? null : allEmployees.get(0);
    }

    // ---- cancel ----

    @Override
    @Scheduled(fixedRate = 60000)
    public void cancelExpiredPendingBookings() {
        bookingRepository.cancelExpiredPendingBookings(OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES));
    }

    @Override
    public void cancelUnpaidBooking(Long bookingId, Long customerId) {
        cancelBooking(bookingId, customerId, null);
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long customerId, BookingCancelDTO dto) {
        Booking b = find(bookingId);
        if (customerId != null && !b.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("Unauthorized action");
        }
        if (!Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(b.getStatus()) && !Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Không thể hủy đơn ở trạng thái: " + b.getStatus());
        }

        boolean refundEligible = false;
        // B7 + B16: Hoàn cọc 100% nếu hủy trước mốc freeCancellationHours theo hợp đồng khách đã đồng ý
        if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) {
            OffsetDateTime depositPaidAt = paymentService.getPaymentsByBooking(bookingId).stream()
                    .filter(p -> Payment.TYPE_DEPOSIT.equalsIgnoreCase(p.getPaymentType())
                            && Payment.STATUS_PAID.equalsIgnoreCase(p.getStatus()))
                    .map(Payment::getPaidAt)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            OffsetDateTime cancelDeadline = contractService.getTermsOfBooking(bookingId)
                    .freeCancellationUntil(b.getPickupTime(), depositPaidAt);
            if (!OffsetDateTime.now().isAfter(cancelDeadline)) {
                refundDeposit(b, dto);
                refundEligible = true;
            }
        }

        b.setStatus(Booking.STATUS_CANCELLED);
        b.setCancelledAt(OffsetDateTime.now());
        bookingRepository.save(b);
        return refundEligible;
    }

    @Override
    public void cancelForUnavailableCar(Long bookingId, String refundMethod) {
        Booking b = find(bookingId);
        if (!Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ huỷ theo cách này với đơn đã đặt cọc và chưa bàn giao (trạng thái đơn: " + b.getStatus() + ")");
        }
        // Only when the company really has no car of the booked type left: otherwise staff swap the car instead,
        // and a customer who no longer wants it cancels under the normal rules.
        if (!availability.findAvailableVehiclesInTimeRange(b.getSlot().getVehicleType().getId(), b.getPickupTime(), b.getScheduledEndTime()).isEmpty()) {
            throw new IllegalStateException("Vẫn còn xe cùng loại trống trong khung giờ thuê: hãy đổi xe cho khách thay vì huỷ đơn.");
        }
        // The contract cannot be fulfilled: the whole deposit goes back at the counter, whatever the cancellation deadline.
        paymentService.recordCounterRefund(bookingId, b.getDepositAmount(), refundMethod);
        b.setStatus(Booking.STATUS_CANCELLED);
        b.setCancelledAt(OffsetDateTime.now());
        bookingRepository.save(b);
    }

    private void refundDeposit(Booking b, BookingCancelDTO dto) {
        String bank = (dto != null && dto.getBankCode() != null && !dto.getBankCode().isBlank()) ? dto.getBankCode().trim() : "BANK";
        String acc = (dto != null && dto.getAccountNumber() != null && !dto.getAccountNumber().isBlank()) ? dto.getAccountNumber().trim() : "";
        String holder = (dto != null && dto.getAccountHolder() != null && !dto.getAccountHolder().isBlank()) ? dto.getAccountHolder().trim().toUpperCase() : "";
        paymentService.createPendingRefund(b.getId(), b.getDepositAmount(), bank, acc, holder);
    }

    // ---- change the car ----

    @Override
    public void assignVehicle(Long bookingId, Long vehicleId) {
        Booking b = find(bookingId);
        // A car can only be swapped before handover: afterwards the customer is driving the assigned one.
        if (!Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(b.getStatus()) && !Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ đổi được xe trước khi bàn giao (trạng thái đơn: " + b.getStatus() + ")");
        }
        Vehicle v = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy xe: " + vehicleId));

        if (!v.getVehicleType().getId().equals(b.getSlot().getVehicleType().getId())) {
            throw new IllegalStateException("Chỉ đổi được sang xe cùng loại với loại xe khách đã đặt.");
        }
        if (Vehicle.STATUS_MAINTENANCE.equalsIgnoreCase(v.getStatus()) || Vehicle.STATUS_RETIRED.equalsIgnoreCase(v.getStatus())) {
            throw new IllegalStateException("Xe này không thể gán cho đơn thuê");
        }

        // B3: Chống gán xe trùng giờ với booking khác (có buffer 1 tiếng)
        OffsetDateTime bufferedStart = b.getPickupTime().minusHours(VehicleAvailabilityService.BUFFER_BETWEEN_RENTALS_HOURS);
        OffsetDateTime bufferedEnd = b.getScheduledEndTime().plusHours(VehicleAvailabilityService.BUFFER_BETWEEN_RENTALS_HOURS);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                bufferedStart, bufferedEnd, OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES), OffsetDateTime.now());
        if (bookedVehicleIds.contains(vehicleId) && (b.getVehicle() == null || !b.getVehicle().getId().equals(vehicleId))) {
            throw new IllegalStateException("Xe này đã có lịch đặt khác trong cùng khung giờ (yêu cầu cách nhau tối thiểu 1 tiếng).");
        }

        b.setVehicle(v);
        bookingRepository.save(b);
    }

    private Booking find(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn đặt xe: " + bookingId));
    }
}
