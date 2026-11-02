package com.crms.mvc.event;

import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.service.IssueService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
                OffsetDateTime.now().minusDays(20), new BigDecimal("800000"))));

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
    void aMailFailureIsSwallowedSoTheCallerIsNeverBroken() {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(issueService.findNotice(7L)).thenThrow(new IllegalStateException("db down"));

        listener(provider, "admin@drivenow.com", "secret").onIssueFeeDue(new IssueFeeDueEvent(7L));

        verify(sender, never()).send(any(MimeMessage.class));
    }

    private IssueFeeNotificationListener listener(ObjectProvider<JavaMailSender> provider, String username, String password) {
        return new IssueFeeNotificationListener(provider, templateEngine, issueService,
                username, password, "DriveNow <admin@drivenow.com>", "http://localhost:8081");
    }
}
