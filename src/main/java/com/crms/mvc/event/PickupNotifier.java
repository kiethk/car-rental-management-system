package com.crms.mvc.event;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The two emails around picking up the car: "your rental time has started" the moment the pickup time arrives on a
 * paid booking, and the notice when such a booking is cancelled because the customer never came (Khách không đến
 * nhận xe). The first picks, each minute, the bookings whose pickup time fell since the previous run, so it is sent
 * exactly once without storing anything; a moment that falls while the app is down is skipped, never sent twice.
 */
@Component
public class PickupNotifier {
    private static final Logger log = LoggerFactory.getLogger(PickupNotifier.class);

    private final BookingRepository bookingRepository;
    private final ContractService contractService;
    private final CustomerMailer mailer;
    private final String stationAddress;
    private OffsetDateTime lastRun = OffsetDateTime.now();

    public PickupNotifier(BookingRepository bookingRepository, ContractService contractService, CustomerMailer mailer,
                          @Value("${crms.station.address:Kho xe DriveNow - 268 Lý Thường Kiệt, P.14, Q.10, TP.HCM}") String stationAddress) {
        this.bookingRepository = bookingRepository;
        this.contractService = contractService;
        this.mailer = mailer;
        this.stationAddress = stationAddress;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional(readOnly = true)
    public void notifyRentalStarted() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = lastRun;
        lastRun = now;
        notifyRentalStartedBetween(from, now);
    }

    /** Emails every paid, not yet handed-over booking whose pickup time came during (from, to]. */
    @Transactional(readOnly = true)
    public int notifyRentalStartedBetween(OffsetDateTime from, OffsetDateTime to) {
        List<Booking> starting = bookingRepository.findConfirmedPickupsBetween(from, to);
        if (starting.isEmpty()) return 0;
        if (!mailer.isConfigured()) {
            log.info("Email service not configured; skipping rental-started email for {} booking(s)", starting.size());
            return 0;
        }
        int sent = 0;
        for (Booking booking : starting) {
            if (booking.getCustomer() == null || booking.getCustomer().getEmail() == null) continue;
            try {
                Map<String, Object> vars = new HashMap<>();
                vars.put("customerName", booking.getCustomer().getName());
                vars.put("bookingId", booking.getId());
                vars.put("vehicleName", booking.getSlot().getVehicleType().getDisplayName());
                vars.put("licensePlate", booking.getVehicle() != null && booking.getVehicle().getLicensePlate() != null
                        ? booking.getVehicle().getLicensePlate().getPlate() : null);
                vars.put("pickupTime", DateTimeUtils.toVietnamTime(booking.getPickupTime()));
                vars.put("scheduledEndTime", DateTimeUtils.toVietnamTime(booking.getScheduledEndTime()));
                vars.put("stationAddress", stationAddress);
                vars.put("pickupWaitHours", contractService.getTermsOfBooking(booking.getId()).getPickupWaitHours());
                vars.put("bookingUrl", mailer.url("/bookings/" + booking.getId()));
                mailer.send(booking.getCustomer().getEmail(),
                        "DriveNow - Giờ thuê xe của đơn #" + booking.getId() + " đã bắt đầu",
                        "mail/rental-started", vars);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send rental-started email for booking #{}: {}", booking.getId(), e.getMessage(), e);
            }
        }
        return sent;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoShowCancelled(NoShowCancelledEvent event) {
        try {
            if (!mailer.isConfigured() || event.customerEmail() == null) {
                log.info("Skipping no-show email for booking #{} (mail not configured or no email)", event.bookingId());
                return;
            }
            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", event.customerName());
            vars.put("bookingId", event.bookingId());
            vars.put("vehicleName", event.vehicleName());
            vars.put("pickupTime", event.pickupTime());
            vars.put("cancelledAt", event.cancelledAt());
            vars.put("depositAmount", event.depositAmount());
            vars.put("pickupWaitHours", event.pickupWaitHours());
            vars.put("bookingUrl", mailer.url("/bookings/" + event.bookingId()));
            mailer.send(event.customerEmail(), "DriveNow - Đơn #" + event.bookingId() + " đã bị hủy vì không nhận xe",
                    "mail/no-show-cancelled", vars);
        } catch (Exception e) {
            log.error("Failed to send no-show email for booking #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }
}
