package com.crms.mvc.notification;

import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.event.IssueCancelledEvent;
import com.crms.mvc.event.IssueFeeDueEvent;
import com.crms.mvc.event.IssueUpdatedEvent;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Emails the customer about an Issue: the fee as soon as it is recorded (with the evidence photos attached), what
 * changed when an announced Issue is edited, and the retraction when it is cancelled. Runs after the commit and off
 * the request thread, and swallows its own failures, so a mail problem can never undo or block the Issue change.
 */
@Component
public class IssueNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(IssueNotificationListener.class);
    private static final String FEE_TEMPLATE = "mail/issue-fee-due";
    private static final String UPDATE_TEMPLATE = "mail/issue-fee-updated";
    private static final String CANCEL_TEMPLATE = "mail/issue-cancelled";

    private final CustomerMailer mailer;
    private final IssueService issueService;
    private final FileUploadService fileUploadService;

    public IssueNotificationListener(CustomerMailer mailer, IssueService issueService, FileUploadService fileUploadService) {
        this.mailer = mailer;
        this.issueService = issueService;
        this.fileUploadService = fileUploadService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueFeeDue(IssueFeeDueEvent event) {
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping fee notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.feeAmount() == null || notice.customerEmail() == null) {
                log.warn("Cannot send fee notice: missing issue, fee or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            Map<String, Resource> photos = evidencePhotos(notice);
            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", notice.customerName());
            vars.put("bookingId", bookingId);
            vars.put("issueType", notice.issueTypeLabel());
            vars.put("subject", notice.subject());
            vars.put("occurredAt", notice.occurredAt());
            vars.put("amount", notice.feeAmount());
            vars.put("bookingUrl", mailer.url("/bookings/" + bookingId));
            vars.put("photoCount", photos.size());
            mailer.send(notice.customerEmail(), "DriveNow - Phí phát sinh cần thanh toán cho đơn #" + bookingId,
                    FEE_TEMPLATE, vars, photos);
            log.info("Sent fee notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send fee notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueUpdated(IssueUpdatedEvent event) {
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping update notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.feeAmount() == null || notice.customerEmail() == null) {
                log.warn("Cannot send update notice: missing issue, fee or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", notice.customerName());
            vars.put("bookingId", bookingId);
            vars.put("issueId", notice.issueId());
            vars.put("issueType", notice.issueTypeLabel());
            vars.put("subject", notice.subject());
            vars.put("occurredAt", notice.occurredAt());
            vars.put("amount", notice.feeAmount());
            vars.put("previousIssueType", event.previousTypeLabel());
            vars.put("previousSubject", event.previousSubject());
            vars.put("previousOccurredAt", event.previousOccurredAt() == null ? null
                    : DateTimeUtils.toVietnamTime(event.previousOccurredAt()));
            vars.put("previousAmount", event.previousFeeAmount());
            vars.put("typeChanged", !Objects.equals(event.previousTypeLabel(), notice.issueTypeLabel()));
            vars.put("subjectChanged", !Objects.equals(event.previousSubject(), notice.subject()));
            vars.put("occurredChanged", event.previousOccurredAt() == null || notice.occurredAt() == null
                    || !event.previousOccurredAt().isEqual(notice.occurredAt()));
            vars.put("amountChanged", event.previousFeeAmount() == null
                    || event.previousFeeAmount().compareTo(notice.feeAmount()) != 0);
            vars.put("bookingUrl", mailer.url("/bookings/" + bookingId));
            mailer.send(notice.customerEmail(),
                    "DriveNow - Cập nhật phí sự cố #" + notice.issueId() + " của đơn #" + bookingId, UPDATE_TEMPLATE, vars);
            log.info("Sent update notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send update notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCancelled(IssueCancelledEvent event) {
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping cancel notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.customerEmail() == null) {
                log.warn("Cannot send cancel notice: missing issue or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", notice.customerName());
            vars.put("bookingId", bookingId);
            vars.put("issueId", notice.issueId());
            vars.put("subject", notice.subject());
            vars.put("cancelReason", event.cancelReason());
            vars.put("bookingUrl", mailer.url("/bookings/" + bookingId));
            mailer.send(notice.customerEmail(),
                    "DriveNow - Thông báo hủy yêu cầu bồi thường sự cố #" + notice.issueId(), CANCEL_TEMPLATE, vars);
            log.info("Sent cancel notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send cancel notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }

    /**
     * The evidence photos still readable on disk, named anh-su-co-1.jpg, anh-su-co-2.png... in the order they were
     * added; a missing file is skipped so it cannot block the notice.
     */
    private Map<String, Resource> evidencePhotos(IssueNoticeDTO notice) {
        List<String> names = notice.evidenceFileNames() == null ? List.of() : notice.evidenceFileNames();
        Map<String, Resource> photos = new LinkedHashMap<>();
        for (String name : names) {
            try {
                Resource photo = fileUploadService.loadIssueEvidence(name);
                if (photo != null && photo.exists()) {
                    photos.put("anh-su-co-" + (photos.size() + 1) + extensionOf(photo.getFilename()), photo);
                }
            } catch (RuntimeException e) {
                log.warn("Could not attach evidence photo {} of issue #{}: {}", name, notice.issueId(), e.getMessage());
            }
        }
        return photos;
    }

    private static String extensionOf(String fileName) {
        return fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".jpg";
    }
}
