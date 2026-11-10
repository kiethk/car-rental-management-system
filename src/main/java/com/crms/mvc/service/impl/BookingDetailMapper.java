package com.crms.mvc.service.impl;

import com.crms.mvc.config.StationProperties;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.util.VehicleImages;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns a booking into what the pages show: the one-line summary of the lists and the full detail of a booking page.
 * Read-only: it asks the payment, contract and fee services for the parts that live elsewhere, and changes nothing.
 */
@Component
public class BookingDetailMapper {
    private static final String LATE_FEE = "LATE_FEE";

    private final PaymentService paymentService;
    private final ContractService contractService;
    private final BookingFeeService bookingFeeService;
    private final PricingService pricingService;
    private final StationProperties station;

    public BookingDetailMapper(PaymentService paymentService,
                               ContractService contractService,
                               BookingFeeService bookingFeeService,
                               PricingService pricingService,
                               StationProperties station) {
        this.paymentService = paymentService;
        this.contractService = contractService;
        this.bookingFeeService = bookingFeeService;
        this.pricingService = pricingService;
        this.station = station;
    }

    /** The row of "my bookings" for a customer. */
    public BookingSummaryDTO toSummary(Booking b) {
        BookingSummaryDTO s = baseSummary(b);
        s.setStatus(b.getStatus());
        return s;
    }

    /** The row of the admin list; a booking waiting for its deposit refund shows as "pending_refund". */
    public BookingSummaryDTO toAdminSummary(Booking b, Set<Long> pendingRefundBookingIds) {
        BookingSummaryDTO s = baseSummary(b);
        boolean pendingRefund = pendingRefundBookingIds.contains(b.getId());
        s.setStatus(pendingRefund ? "pending_refund" : b.getStatus());
        if (pendingRefund) {
            s.setHasPendingRefund(true);
            s.setRefundAmount(b.getDepositAmount());
        }
        return s;
    }

    private BookingSummaryDTO baseSummary(Booking b) {
        BookingSummaryDTO s = new BookingSummaryDTO();
        s.setId(b.getId());
        VehicleType vt = b.getSlot() != null ? b.getSlot().getVehicleType() : null;
        if (vt != null) {
            s.setVehicleTypeName(vt.getName());
            s.setVehicleCategory(vt.getCategory());
            s.setImageUrl(VehicleImages.urlOf(vt));
        } else {
            s.setVehicleTypeName("Phương tiện DriveNow");
        }
        s.setLicensePlate(resolvePlate(b));
        s.setPickupTime(DateTimeUtils.toVietnamTime(b.getPickupTime()));
        s.setScheduledEndTime(DateTimeUtils.toVietnamTime(b.getScheduledEndTime()));
        s.setTotalPrice(b.getTotalPrice());
        s.setDepositAmount(b.getDepositAmount());
        s.setCustomerName(b.getCustomer() != null ? b.getCustomer().getName() : null);
        s.setOverdueMinutes(overdueMinutes(b, OffsetDateTime.now()));
        s.setPickupDeadline(awaitingPickupUntil(b, OffsetDateTime.now()));
        return s;
    }

    /** Everything a booking page needs: customer, car, times, money, payments, contract, late return, fees, staff. */
    public BookingDetailDTO toDetail(Booking b) {
        BookingDetailDTO d = new BookingDetailDTO();
        fillCustomerAndVehicle(d, b);
        fillTimesAndPrice(d, b);
        fillPaymentsAndContract(d, b);
        fillFees(d, b);
        fillStaff(d, b);
        return d;
    }

    private void fillCustomerAndVehicle(BookingDetailDTO d, Booking b) {
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
        d.setImageUrl(VehicleImages.urlOf(vt));

        if (b.getVehicle() != null) {
            d.setVehicleId(b.getVehicle().getId());
            d.setVehicleColor(b.getVehicle().getColor());
            d.setVehicleRetired(Vehicle.STATUS_RETIRED.equalsIgnoreCase(b.getVehicle().getStatus()));
            d.setVehicleStatus(b.getVehicle().getStatus());
            d.setVehicleOdometer(b.getVehicle().getOdometer());
        }
        d.setLicensePlate(resolvePlate(b));
    }

    private void fillTimesAndPrice(BookingDetailDTO d, Booking b) {
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
    }

    private void fillPaymentsAndContract(BookingDetailDTO d, Booking b) {
        List<BookingDetailDTO.PaymentDTO> payments = new ArrayList<>();
        for (Payment p : paymentService.getPaymentsByBooking(b.getId())) {
            BookingDetailDTO.PaymentDTO pd = new BookingDetailDTO.PaymentDTO();
            pd.setId(p.getId());
            pd.setTransactionCode(p.getTransactionCode());
            pd.setPaymentType(p.getPaymentType());
            pd.setAmount(p.getAmount());
            pd.setPaymentMethod(p.getPaymentMethod());
            pd.setStatus(p.getStatus());
            pd.setPaidAt(DateTimeUtils.toVietnamTime(p.getPaidAt()));
            payments.add(pd);
        }
        d.setPayments(payments);

        contractService.findByBookingId(b.getId()).ifPresent(d::setContract);
        ContractTemplateDTO terms = d.getContract() != null ? d.getContract().getTerms() : null;
        if (terms != null) {
            d.setPickupDeadline(awaitingPickupUntil(b, terms, OffsetDateTime.now()));
        }
        if (isOverdue(b, OffsetDateTime.now())) {
            BigDecimal multiplier = terms != null ? terms.getLateFeeMultiplier() : null;
            BigDecimal rate = b.getPricePerHour() != null ? b.getPricePerHour() : b.getSlot().getVehicleType().getPricePerHour();
            d.setLateReturn(pricingService.describeLateReturn(b.getScheduledEndTime(), OffsetDateTime.now(), rate, multiplier));
        }
    }

    private void fillFees(BookingDetailDTO d, Booking b) {
        List<BookingDetailDTO.BookingFeeDTO> feeDtos = new ArrayList<>();
        BigDecimal lateFeeTotal = BigDecimal.ZERO;
        for (BookingFee fee : bookingFeeService.getFeesByBooking(b.getId())) {
            BookingDetailDTO.BookingFeeDTO fd = new BookingDetailDTO.BookingFeeDTO();
            fd.setId(fee.getId());
            fd.setFeeType(fee.getFeeType());
            fd.setAmount(fee.getAmount());
            fd.setDescription(fee.getDescription());
            fd.setIssueId(fee.getIssue() != null ? fee.getIssue().getId() : null);
            fd.setCreatedAt(fee.getCreatedAt());
            feeDtos.add(fd);
            if (LATE_FEE.equalsIgnoreCase(fee.getFeeType())) {
                lateFeeTotal = lateFeeTotal.add(fee.getAmount());
            }
        }
        d.setFees(feeDtos);
        d.setLateFee(lateFeeTotal);
    }

    private void fillStaff(BookingDetailDTO d, Booking b) {
        if (b.getEmployee() != null) {
            d.setStaffId(b.getEmployee().getId());
            d.setStaffName(b.getEmployee().getName());
            d.setStaffPhone(b.getEmployee().getPhone());
            d.setStaffEmail(b.getEmployee().getEmail());
        } else {
            d.setStaffName("Bãi xe DriveNow (Quản lý)");
            d.setStaffPhone(station.phone());
        }
        d.setStationAddress(station.address());
    }

    /** The plate of the car assigned to the booking, read from the car itself. */
    private static String resolvePlate(Booking b) {
        if (b.getVehicle() == null) {
            return "Unassigned";
        }
        return b.getVehicle().getLicensePlate() != null ? b.getVehicle().getLicensePlate().getPlate() : "N/A";
    }

    /** While a paid booking waits for its customer after the pickup time: when it will be cancelled; else null. */
    private OffsetDateTime awaitingPickupUntil(Booking b, OffsetDateTime now) {
        if (!Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus()) || b.getPickupTime() == null || now.isBefore(b.getPickupTime())) {
            return null;
        }
        return awaitingPickupUntil(b, contractService.getTermsOfBooking(b.getId()), now);
    }

    private static OffsetDateTime awaitingPickupUntil(Booking b, ContractTemplateDTO terms, OffsetDateTime now) {
        if (!Booking.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus()) || b.getPickupTime() == null || now.isBefore(b.getPickupTime())) {
            return null;
        }
        return DateTimeUtils.toVietnamTime(terms.pickupDeadline(b.getPickupTime(), b.getScheduledEndTime()));
    }

    /** An active rental whose car has not come back although its scheduled return time has passed. */
    static boolean isOverdue(Booking b, OffsetDateTime now) {
        return Booking.STATUS_ACTIVE.equalsIgnoreCase(b.getStatus()) && b.getActualReturnTime() == null
                && b.getScheduledEndTime() != null && now.isAfter(b.getScheduledEndTime());
    }

    private static Long overdueMinutes(Booking b, OffsetDateTime now) {
        return isOverdue(b, now) ? Duration.between(b.getScheduledEndTime(), now).toMinutes() : null;
    }
}
