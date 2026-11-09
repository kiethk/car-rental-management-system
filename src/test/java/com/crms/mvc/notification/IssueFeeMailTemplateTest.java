package com.crms.mvc.notification;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The listener tests mock the template engine, so this renders the real email template once. */
class IssueFeeMailTemplateTest {

    @Test
    void theEmailShowsWhatIsOwedAndLinksToTheBooking() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context(Locale.of("vi", "VN"));
        context.setVariable("customerName", "Nguyễn Văn An");
        context.setVariable("bookingId", 42L);
        context.setVariable("issueType", "Phạt nguội");
        context.setVariable("subject", "Vượt đèn đỏ");
        context.setVariable("occurredAt", OffsetDateTime.parse("2026-10-20T14:30:00+07:00"));
        context.setVariable("amount", new BigDecimal("800000"));
        context.setVariable("bookingUrl", "http://localhost:8081/bookings/42");

        String html = engine.process("mail/issue-fee-due", context);

        assertTrue(html.contains("Nguyễn Văn An"), html);
        assertTrue(html.contains("#42"), html);
        assertTrue(html.contains("Vượt đèn đỏ"), html);
        assertTrue(html.contains("800.000 đ"), html);
        assertTrue(html.contains("14:30, 20/10/2026"), html);
        assertTrue(html.contains("href=\"http://localhost:8081/bookings/42\""), html);
    }
}
