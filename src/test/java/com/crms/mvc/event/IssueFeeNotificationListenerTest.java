package com.crms.mvc.event;

import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.IssueService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The customer is emailed what an Issue costs; a mail problem must never break the Issue change. */
class IssueFeeNotificationListenerTest {
    private final IssueService issueService = mock(IssueService.class);
    private final ITemplateEngine templateEngine = mock(ITemplateEngine.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);

    @Test
    @SuppressWarnings("unchecked")
    void staysQuietWhenEmailIsNotConfigured() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        IssueFeeNotificationListener listener = listener(provider, "", "");

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));

        verifyNoInteractions(issueService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emailsTheCustomerWhatIsOwedAndWhereToPay() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("mail/issue-fee-due"), any(Context.class))).thenReturn("<html>fee</html>");
        when(issueService.findNotice(7L)).thenReturn(Optional.of(new IssueNoticeDTO(
                7L, 42L, "Nguyễn Văn An", "an@example.com", "Phạt nguội", "Red light, Le Loi street",
                OffsetDateTime.now().minusDays(20), new BigDecimal("800000"), List.of())));

        listener(provider, "admin@drivenow.com", "secret").onIssueFeeDue(new IssueFeeDueEvent(7L));

        verify(sender).send(message);
        assertEquals("an@example.com", message.getAllRecipients()[0].toString());
        assertTrue(message.getSubject().contains("#42"));

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/issue-fee-due"), context.capture());
        assertEquals("Red light, Le Loi street", context.getValue().getVariable("subject"));
        assertEquals(new BigDecimal("800000"), context.getValue().getVariable("amount"));
        assertEquals("http://localhost:8081/bookings/42", context.getValue().getVariable("bookingUrl"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theEvidencePhotosAreAttachedToTheFirstNotice() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("mail/issue-fee-due"), any(Context.class))).thenReturn("<html>fee</html>");
        when(issueService.findNotice(7L)).thenReturn(Optional.of(new IssueNoticeDTO(
                7L, 42L, "Nguyễn Văn An", "an@example.com", "Hư hỏng", "Scratch",
                OffsetDateTime.now().minusDays(1), new BigDecimal("200000"), List.of("issue-a.jpg", "issue-gone.png"))));
        when(fileUploadService.loadIssueEvidence("issue-a.jpg")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override public String getFilename() { return "issue-a.jpg"; }
        });
        when(fileUploadService.loadIssueEvidence("issue-gone.png")).thenThrow(new NoSuchElementException("missing"));

        listener(provider, "admin@drivenow.com", "secret").onIssueFeeDue(new IssueFeeDueEvent(7L));

        verify(sender).send(message);
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/issue-fee-due"), context.capture());
        assertEquals(1, context.getValue().getVariable("photoCount"));
        message.saveChanges();
        jakarta.mail.internet.MimeMultipart root = (jakarta.mail.internet.MimeMultipart) message.getContent();
        assertTrue(attachmentNames(root).contains("anh-su-co-1.jpg"), attachmentNames(root).toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUpdateEmailSaysWhatChanged() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("mail/issue-fee-updated"), any(Context.class))).thenReturn("<html>updated</html>");
        OffsetDateTime occurred = OffsetDateTime.now().minusDays(1);
        when(issueService.findNotice(7L)).thenReturn(Optional.of(new IssueNoticeDTO(
                7L, 42L, "Nguyễn Văn An", "an@example.com", "Hư hỏng", "Dented bumper",
                occurred, new BigDecimal("350000"), List.of())));

        listener(provider, "admin@drivenow.com", "secret").onIssueUpdated(new IssueUpdatedEvent(
                7L, "Hư hỏng", "Scratch", null, occurred, new BigDecimal("200000")));

        verify(sender).send(message);
        assertTrue(message.getSubject().contains("Cập nhật"), message.getSubject());
        assertTrue(message.getSubject().contains("#7"), message.getSubject());
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/issue-fee-updated"), context.capture());
        assertEquals(true, context.getValue().getVariable("amountChanged"));
        assertEquals(true, context.getValue().getVariable("subjectChanged"));
        assertEquals(false, context.getValue().getVariable("typeChanged"));
        assertEquals(false, context.getValue().getVariable("occurredChanged"));
        assertEquals(new BigDecimal("200000"), context.getValue().getVariable("previousAmount"));
        assertEquals("Scratch", context.getValue().getVariable("previousSubject"));
        assertEquals(new BigDecimal("350000"), context.getValue().getVariable("amount"));
    }

    private static List<String> attachmentNames(jakarta.mail.internet.MimeMultipart multipart) throws Exception {
        List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            jakarta.mail.BodyPart part = multipart.getBodyPart(i);
            if (part.getFileName() != null) names.add(part.getFileName());
            if (part.getContent() instanceof jakarta.mail.internet.MimeMultipart nested) names.addAll(attachmentNames(nested));
        }
        return names;
    }

    @Test
    @SuppressWarnings("unchecked")
    void aMailFailureIsSwallowedSoTheCallerIsNeverBroken() {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(issueService.findNotice(7L)).thenThrow(new IllegalStateException("db down"));

        listener(provider, "admin@drivenow.com", "secret").onIssueFeeDue(new IssueFeeDueEvent(7L));

        verify(sender, never()).send(any(MimeMessage.class));
    }

    private IssueFeeNotificationListener listener(ObjectProvider<JavaMailSender> provider, String username, String password) {
        return new IssueFeeNotificationListener(provider, templateEngine, issueService, fileUploadService,
                username, password, "DriveNow <admin@drivenow.com>", "http://localhost:8081");
    }
}
