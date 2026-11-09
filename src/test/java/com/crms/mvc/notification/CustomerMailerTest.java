package com.crms.mvc.notification;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The one place that turns a template into a customer email: sender, recipient, subject, body and attachments. */
class CustomerMailerTest {

    @Test
    @SuppressWarnings("unchecked")
    void rendersTheTemplateAndSendsItToTheCustomer() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(provider.getObject()).thenReturn(sender);
        CustomerMailer mailer = new CustomerMailer(provider, engine(), "admin@drivenow.com", "secret", "", "http://localhost:8081");

        mailer.send("an@example.com", "DriveNow - Cảm ơn", "mail/payment-thank-you", variables());

        verify(sender).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("an@example.com");
        assertThat(message.getSubject()).isEqualTo("DriveNow - Cảm ơn");
        assertThat(message.getFrom()[0].toString()).contains("admin@drivenow.com").contains("DriveNow");
        message.saveChanges();
        assertThat(text(message)).contains("Nguyễn Văn An").contains("500.000 đ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void attachmentsAreAddedUnderTheirOwnNames() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(sender);
        CustomerMailer mailer = new CustomerMailer(provider, engine(), "admin@drivenow.com", "secret",
                "DriveNow <no-reply@drivenow.com>", "http://localhost:8081");
        Map<String, org.springframework.core.io.Resource> files = new LinkedHashMap<>();
        files.put("anh-su-co-1.jpg", new ByteArrayResource(new byte[]{1, 2, 3}));
        files.put("anh-su-co-2.png", new ByteArrayResource(new byte[]{4, 5}));

        mailer.send("an@example.com", "Subject", "mail/payment-thank-you", variables(), files);

        message.saveChanges();
        assertThat(attachmentNames((MimeMultipart) message.getContent())).containsExactly("anh-su-co-1.jpg", "anh-su-co-2.png");
        assertThat(message.getFrom()[0].toString()).isEqualTo("DriveNow <no-reply@drivenow.com>");
    }

    @Test
    @SuppressWarnings("unchecked")
    void isConfiguredOnlyWithAMailSenderAndCredentials() {
        ObjectProvider<JavaMailSender> none = mock(ObjectProvider.class);
        ObjectProvider<JavaMailSender> some = mock(ObjectProvider.class);
        when(some.getIfAvailable()).thenReturn(mock(JavaMailSender.class));

        assertThat(new CustomerMailer(none, engine(), "a", "b", "", "").isConfigured()).isFalse();
        assertThat(new CustomerMailer(some, engine(), "", "b", "", "").isConfigured()).isFalse();
        assertThat(new CustomerMailer(some, engine(), "a", "", "", "").isConfigured()).isFalse();
        assertThat(new CustomerMailer(some, engine(), "a", "b", "", "").isConfigured()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void urlBuildsAnAbsoluteLinkIntoTheApp() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        CustomerMailer mailer = new CustomerMailer(provider, engine(), "a", "b", "", "https://drivenow.example");

        assertThat(mailer.url("/bookings/42")).isEqualTo("https://drivenow.example/bookings/42");
    }

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static Map<String, Object> variables() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerName", "Nguyễn Văn An");
        vars.put("bookingId", 42L);
        vars.put("vehicleName", "Toyota Vios");
        vars.put("amount", new BigDecimal("500000"));
        vars.put("paymentMethod", "Tiền mặt");
        vars.put("paidAt", OffsetDateTime.parse("2026-11-08T14:30:00+07:00"));
        vars.put("kind", "RENTAL_BALANCE");
        vars.put("bookingClosed", false);
        vars.put("bookingUrl", "http://localhost:8081/bookings/42");
        return vars;
    }

    private static String text(MimeMessage message) throws Exception {
        return flatten(message.getContent());
    }

    private static String flatten(Object content) throws Exception {
        if (content instanceof String s) return s;
        StringBuilder out = new StringBuilder();
        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) out.append(flatten(multipart.getBodyPart(i).getContent()));
        }
        return out.toString();
    }

    private static List<String> attachmentNames(MimeMultipart multipart) throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getFileName() != null) names.add(part.getFileName());
            if (part.getContent() instanceof MimeMultipart nested) names.addAll(attachmentNames(nested));
        }
        return names;
    }
}
