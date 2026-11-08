package com.crms.mvc.event;

import com.crms.mvc.dto.LateReturnDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.util.DateTimeUtils;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Emails a customer whose car is not back at most twice: a reminder as soon as the scheduled return time passes
 * (when the late fee will start and how much one hour costs), and a notice when the first late hour starts to be
 * charged one hour later (see GLOSSARY "Ân hạn trả xe"). Each minute it picks the rentals whose moment fell since the
 * previous run, so each email goes out exactly once without storing anything; a moment that falls while the app is
 * down is skipped, never sent twice. The app pages show the live late time and fee after that.
 */
@Component
public class LateReturnNotifier {
    private static final Logger log = LoggerFactory.getLogger(LateReturnNotifier.class);
    private static final String TEMPLATE = "mail/late-return";
    private static final String REMINDER_TEMPLATE = "mail/late-return-reminder";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final BookingRepository bookingRepository;
    private final ContractService contractService;
    private final PricingService pricingService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String baseUrl;
    private OffsetDateTime lastRun = OffsetDateTime.now();

    public LateReturnNotifier(BookingRepository bookingRepository,
                              ContractService contractService,
                              PricingService pricingService,
                              ObjectProvider<JavaMailSender> mailSender,
                              ITemplateEngine templateEngine,
                              @Value("${spring.mail.username:}") String username,
                              @Value("${spring.mail.password:}") String password,
                              @Value("${crms.mail.from:}") String fromAddress,
                              @Value("${crms.base-url:http://localhost:8081}") String baseUrl) {
        this.bookingRepository = bookingRepository;
        this.contractService = contractService;
        this.pricingService = pricingService;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional(readOnly = true)
    public void notifyFirstChargedLateHour() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = lastRun;
        lastRun = now;
        notifyReturnTimePassedBetween(from, now);
        notifyFirstChargedLateHourBetween(from, now);
    }

    /** Reminds every rental still out whose scheduled return time passed during (from, to]. */
    @Transactional(readOnly = true)
    public int notifyReturnTimePassedBetween(OffsetDateTime from, OffsetDateTime to) {
        return sendAll(bookingRepository.findRentalsDueBetween(from, to), REMINDER_TEMPLATE,
                "DriveNow - Đơn #%d đã quá giờ trả xe: phí trễ sẽ bắt đầu tính sau 1 giờ");
    }

    /** Mails every rental still out whose first late hour started to be charged during (from, to]. */
    @Transactional(readOnly = true)
    public int notifyFirstChargedLateHourBetween(OffsetDateTime from, OffsetDateTime to) {
        // The first late hour is charged one hour after the scheduled return time.
        return sendAll(bookingRepository.findRentalsDueBetween(from.minusHours(1), to.minusHours(1)), TEMPLATE,
                "DriveNow - Đơn #%d đã quá giờ trả xe và bắt đầu tính phí trễ");
    }

    private int sendAll(List<Booking> due, String template, String subject) {
        if (due.isEmpty()) return 0;

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            log.info("Email service not configured; skipping {} for {} booking(s)", template, due.size());
            return 0;
        }
        int sent = 0;
        for (Booking booking : due) {
            try {
                send(sender, booking, template, String.format(subject, booking.getId()));
                sent++;
            } catch (Exception e) {
                log.error("Failed to send late-return notice for booking #{}: {}", booking.getId(), e.getMessage(), e);
            }
        }
        return sent;
    }

    private void send(JavaMailSender sender, Booking booking, String template, String subject) throws Exception {
        if (booking.getCustomer() == null || booking.getCustomer().getEmail() == null) {
            log.warn("Cannot send late-return notice: no customer email for booking #{}", booking.getId());
            return;
        }
        BigDecimal multiplier = contractService.getTermsOfBooking(booking.getId()).getLateFeeMultiplier();
        BigDecimal rate = booking.getPricePerHour() != null ? booking.getPricePerHour()
                : booking.getSlot().getVehicleType().getPricePerHour();
        OffsetDateTime firstCharge = booking.getScheduledEndTime().plusHours(1);
        LateReturnDTO late = pricingService.describeLateReturn(booking.getScheduledEndTime(), firstCharge, rate, multiplier);

        Context context = new Context(VIETNAMESE);
        context.setVariable("customerName", booking.getCustomer().getName());
        context.setVariable("bookingId", booking.getId());
        context.setVariable("vehicleName", booking.getSlot().getVehicleType().getDisplayName());
        context.setVariable("licensePlate", booking.getVehicle() != null && booking.getVehicle().getLicensePlate() != null
                ? booking.getVehicle().getLicensePlate().getPlate() : null);
        context.setVariable("scheduledEndTime", DateTimeUtils.toVietnamTime(booking.getScheduledEndTime()));
        context.setVariable("hourFee", late.currentFee());
        context.setVariable("firstChargeAt", DateTimeUtils.toVietnamTime(firstCharge));
        context.setVariable("nextChargeAt", DateTimeUtils.toVietnamTime(late.nextChargeAt()));
        context.setVariable("bookingUrl", baseUrl + "/bookings/" + booking.getId());
        String body = templateEngine.process(template, context);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
        helper.setTo(booking.getCustomer().getEmail());
        helper.setSubject(subject);
        helper.setText(body, true);
        sender.send(message);
        log.info("Sent {} for booking #{}", template, booking.getId());
    }
}
