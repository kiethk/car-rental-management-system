package com.crms.mvc.event;

import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.util.DateTimeUtils;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Emails the customer about an Issue fee (as soon as it is recorded, with the evidence photos attached),
 * when an announced Issue is edited (saying what changed) and when it is cancelled. Runs after the commit and off the request thread, and swallows its own
 * failures, so a mail problem can never undo or block the Issue change.
 */
@Component
public class IssueFeeNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(IssueFeeNotificationListener.class);
    private static final String TEMPLATE = "mail/issue-fee-due";
    private static final String UPDATE_TEMPLATE = "mail/issue-fee-updated";
    private static final String CANCEL_TEMPLATE = "mail/issue-cancelled";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final IssueService issueService;
    private final FileUploadService fileUploadService;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String baseUrl;

    public IssueFeeNotificationListener(ObjectProvider<JavaMailSender> mailSender,
                                        ITemplateEngine templateEngine,
                                        IssueService issueService,
                                        FileUploadService fileUploadService,
                                        @Value("${spring.mail.username:}") String username,
                                        @Value("${spring.mail.password:}") String password,
                                        @Value("${crms.mail.from:}") String fromAddress,
                                        @Value("${crms.base-url:http://localhost:8081}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.issueService = issueService;
        this.fileUploadService = fileUploadService;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueFeeDue(IssueFeeDueEvent event) {
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
                log.info("Email service not configured; skipping fee notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.feeAmount() == null || notice.customerEmail() == null) {
                log.warn("Cannot send fee notice: missing issue, fee or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            Context context = new Context(VIETNAMESE);
            context.setVariable("customerName", notice.customerName());
            context.setVariable("bookingId", bookingId);
            context.setVariable("issueType", notice.issueTypeLabel());
            context.setVariable("subject", notice.subject());
            context.setVariable("occurredAt", notice.occurredAt());
            context.setVariable("amount", notice.feeAmount());
            context.setVariable("bookingUrl", baseUrl + "/bookings/" + bookingId);
            List<Resource> photos = evidencePhotos(notice);
            context.setVariable("photoCount", photos.size());
            String body = templateEngine.process(TEMPLATE, context);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(notice.customerEmail());
            helper.setSubject("DriveNow - Phí phát sinh cần thanh toán cho đơn #" + bookingId);
            helper.setText(body, true);
            for (int i = 0; i < photos.size(); i++) {
                helper.addAttachment("anh-su-co-" + (i + 1) + extensionOf(photos.get(i).getFilename()), photos.get(i));
            }
            sender.send(message);
            log.info("Sent fee notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send fee notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueUpdated(IssueUpdatedEvent event) {
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
                log.info("Email service not configured; skipping update notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.feeAmount() == null || notice.customerEmail() == null) {
                log.warn("Cannot send update notice: missing issue, fee or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            boolean typeChanged = !Objects.equals(event.previousTypeLabel(), notice.issueTypeLabel());
            boolean subjectChanged = !Objects.equals(event.previousSubject(), notice.subject());
            boolean occurredChanged = event.previousOccurredAt() == null || notice.occurredAt() == null
                    || !event.previousOccurredAt().isEqual(notice.occurredAt());
            boolean amountChanged = event.previousFeeAmount() == null
                    || event.previousFeeAmount().compareTo(notice.feeAmount()) != 0;

            Context context = new Context(VIETNAMESE);
            context.setVariable("customerName", notice.customerName());
            context.setVariable("bookingId", bookingId);
            context.setVariable("issueId", notice.issueId());
            context.setVariable("issueType", notice.issueTypeLabel());
            context.setVariable("subject", notice.subject());
            context.setVariable("occurredAt", notice.occurredAt());
            context.setVariable("amount", notice.feeAmount());
            context.setVariable("previousIssueType", event.previousTypeLabel());
            context.setVariable("previousSubject", event.previousSubject());
            context.setVariable("previousOccurredAt", event.previousOccurredAt() == null ? null
                    : DateTimeUtils.toVietnamTime(event.previousOccurredAt()));
            context.setVariable("previousAmount", event.previousFeeAmount());
            context.setVariable("typeChanged", typeChanged);
            context.setVariable("subjectChanged", subjectChanged);
            context.setVariable("occurredChanged", occurredChanged);
            context.setVariable("amountChanged", amountChanged);
            context.setVariable("bookingUrl", baseUrl + "/bookings/" + bookingId);
            String body = templateEngine.process(UPDATE_TEMPLATE, context);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(notice.customerEmail());
            helper.setSubject("DriveNow - Cập nhật phí sự cố #" + notice.issueId() + " của đơn #" + bookingId);
            helper.setText(body, true);
            sender.send(message);
            log.info("Sent update notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send update notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }

    /** The evidence photos still readable on disk; a missing file is skipped so it cannot block the notice. */
    private List<Resource> evidencePhotos(IssueNoticeDTO notice) {
        List<String> names = notice.evidenceFileNames() == null ? List.of() : notice.evidenceFileNames();
        List<Resource> photos = new ArrayList<>();
        for (String name : names) {
            try {
                Resource photo = fileUploadService.loadIssueEvidence(name);
                if (photo != null && photo.exists()) {
                    photos.add(photo);
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCancelled(IssueCancelledEvent event) {
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
                log.info("Email service not configured; skipping cancel notice for issue #{}", event.issueId());
                return;
            }
            IssueNoticeDTO notice = issueService.findNotice(event.issueId()).orElse(null);
            if (notice == null || notice.customerEmail() == null) {
                log.warn("Cannot send cancel notice: missing issue or customer email for issue #{}", event.issueId());
                return;
            }

            Long bookingId = notice.bookingId();
            Context context = new Context(VIETNAMESE);
            context.setVariable("customerName", notice.customerName());
            context.setVariable("bookingId", bookingId);
            context.setVariable("issueId", notice.issueId());
            context.setVariable("subject", notice.subject());
            context.setVariable("cancelReason", event.cancelReason());
            context.setVariable("bookingUrl", baseUrl + "/bookings/" + bookingId);
            String body = templateEngine.process(CANCEL_TEMPLATE, context);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(notice.customerEmail());
            helper.setSubject("DriveNow - Thông báo hủy yêu cầu bồi thường sự cố #" + notice.issueId());
            helper.setText(body, true);
            sender.send(message);
            log.info("Sent cancel notice for issue #{} to booking #{}", event.issueId(), bookingId);
        } catch (Exception e) {
            log.error("Failed to send cancel notice for issue #{}: {}", event.issueId(), e.getMessage(), e);
        }
    }
}
