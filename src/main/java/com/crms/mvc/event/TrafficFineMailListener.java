package com.crms.mvc.event;

import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Emails the customer when a traffic fine is recorded or removed. Runs after the commit and off the request
 * thread, and swallows its own failures, so a mail problem can never undo or block the record.
 */
@Component
public class TrafficFineMailListener {
    private static final Logger log = LoggerFactory.getLogger(TrafficFineMailListener.class);

    private final CustomerMailer mailer;
    private final FileUploadService fileUploadService;

    public TrafficFineMailListener(CustomerMailer mailer, FileUploadService fileUploadService) {
        this.mailer = mailer;
        this.fileUploadService = fileUploadService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecorded(TrafficFineRecordedEvent event) {
        send(event.notice(), "mail/traffic-fine-recorded",
                "DriveNow - Thông báo vi phạm giao thông trong đơn thuê #" + event.notice().bookingId(),
                policeNoticePhoto(event.imageFileName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoved(TrafficFineRemovedEvent event) {
        send(event.notice(), "mail/traffic-fine-removed",
                "DriveNow - Gỡ thông báo vi phạm giao thông của đơn thuê #" + event.notice().bookingId(),
                Map.of());
    }

    /** The police notice photo as an attachment named after its type, or nothing when the file cannot be read. */
    private Map<String, Resource> policeNoticePhoto(String imageFileName) {
        try {
            Resource photo = fileUploadService.loadTrafficFineImage(imageFileName);
            String extension = imageFileName.contains(".") ? imageFileName.substring(imageFileName.lastIndexOf('.')) : ".jpg";
            return Map.of("bien-ban-phat-nguoi" + extension, photo);
        } catch (RuntimeException e) {
            log.warn("Could not attach the police notice photo {}: {}", imageFileName, e.getMessage());
            return Map.of();
        }
    }

    private void send(TrafficFineNoticeDTO notice, String template, String subject, Map<String, Resource> attachments) {
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping traffic fine mail for booking #{}", notice.bookingId());
                return;
            }
            if (notice.customerEmail() == null || notice.customerEmail().isBlank()) {
                log.warn("Cannot send traffic fine mail: booking #{} has no customer email", notice.bookingId());
                return;
            }
            mailer.send(notice.customerEmail(), subject, template, Map.of(
                    "notice", notice,
                    "bookingUrl", mailer.url("/bookings/" + notice.bookingId()),
                    "hasAttachment", !attachments.isEmpty()), attachments);
        } catch (Exception e) {
            log.warn("Could not send traffic fine mail for booking #{}: {}", notice.bookingId(), e.getMessage());
        }
    }
}
