package com.crms.mvc.event;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.IssueRepository;
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
import java.util.List;
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

/** The customer is emailed when a fee falls due after settlement; a mail problem must never break resolving. */
class IssueFeeNotificationListenerTest {
    private final IssueRepository issueRepository = mock(IssueRepository.class);
    private final BookingFeeRepository feeRepository = mock(BookingFeeRepository.class);
    private final ITemplateEngine templateEngine = mock(ITemplateEngine.class);

    @Test
    @SuppressWarnings("unchecked")
    void staysQuietWhenEmailIsNotConfigured() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        IssueFeeNotificationListener listener = listener(provider, "", "");

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));

        verifyNoInteractions(issueRepository);
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

        Customer customer = new Customer();
        customer.setName("Nguyễn Văn An");
        customer.setEmail("an@example.com");
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        Issue issue = new Issue();
        issue.setId(7L);
        issue.setBooking(booking);
        issue.setIssueType(IssueType.TRAFFIC_FINE);
        issue.setSubject("Red light, Le Loi street");
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setOccurredAt(OffsetDateTime.now().minusDays(20));
        BookingFee fee = new BookingFee();
        fee.setAmount(new BigDecimal("800000"));
        when(issueRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(issue));
        when(feeRepository.findByIssueId(7L)).thenReturn(List.of(fee));

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
        when(issueRepository.findByIdWithDetails(7L)).thenThrow(new IllegalStateException("db down"));

        listener(provider, "admin@drivenow.com", "secret").onIssueFeeDue(new IssueFeeDueEvent(7L));

        verify(sender, never()).send(any(MimeMessage.class));
    }

    private IssueFeeNotificationListener listener(ObjectProvider<JavaMailSender> provider, String username, String password) {
        return new IssueFeeNotificationListener(provider, templateEngine, issueRepository, feeRepository,
                username, password, "DriveNow <admin@drivenow.com>", "http://localhost:8081");
    }
}
