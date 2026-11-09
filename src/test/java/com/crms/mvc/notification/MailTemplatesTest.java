package com.crms.mvc.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The listener tests mock the template engine, so these render the real customer emails once. */
class MailTemplatesTest {
    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    @Test
    void theUpdateEmailSaysItIsAnAdjustmentAndShowsOldAndNewFee() {
        Context context = new Context(Locale.of("vi", "VN"));
        context.setVariable("customerName", "Nguyễn Văn An");
        context.setVariable("bookingId", 42L);
        context.setVariable("issueId", 7L);
        context.setVariable("issueType", "Hư hỏng");
        context.setVariable("subject", "Móp cản trước");
        context.setVariable("occurredAt", OffsetDateTime.parse("2026-10-20T14:30:00+07:00"));
        context.setVariable("amount", new BigDecimal("350000"));
        context.setVariable("previousIssueType", "Hư hỏng");
        context.setVariable("previousSubject", "Trầy cản trước");
        context.setVariable("previousOccurredAt", OffsetDateTime.parse("2026-10-20T14:30:00+07:00"));
        context.setVariable("previousAmount", new BigDecimal("200000"));
        context.setVariable("typeChanged", false);
        context.setVariable("subjectChanged", true);
        context.setVariable("occurredChanged", false);
        context.setVariable("amountChanged", true);
        context.setVariable("bookingUrl", "http://localhost:8081/bookings/42");

        String html = engine.process("mail/issue-fee-updated", context);

        assertTrue(html.contains("thông báo điều chỉnh"), html);
        assertTrue(html.contains("sự cố #7"), html);
        assertTrue(html.contains("200.000 đ"), html);
        assertTrue(html.contains("350.000 đ"), html);
        assertTrue(html.contains("Trầy cản trước"), html);
        assertTrue(html.contains("Móp cản trước"), html);
        assertTrue(html.contains("đã được huỷ"), html);
    }

    @Test
    void theFirstFeeEmailMentionsTheAttachedPhotosOnlyWhenThereAreSome() {
        String withPhotos = engine.process("mail/issue-fee-due", feeContext(2));
        String withoutPhotos = engine.process("mail/issue-fee-due", feeContext(0));

        assertTrue(withPhotos.contains("2 ảnh"), withPhotos);
        assertTrue(withPhotos.contains("đính kèm"), withPhotos);
        assertFalse(withoutPhotos.contains("chụp sự cố trong email này"), withoutPhotos);
    }

    @Test
    void theTrafficFineEmailTellsTheCustomerTheFineFileIsAttached() {
        String html = engine.process("mail/traffic-fine-recorded", fineContext(true));
        String without = engine.process("mail/traffic-fine-recorded", fineContext(false));

        assertTrue(html.contains("có đính kèm tệp biên bản phạt nguội"), html);
        assertTrue(html.contains("bien-ban-phat-nguoi.jpg"), html);
        assertFalse(without.contains("có đính kèm tệp biên bản phạt nguội"), without);
    }

    @Test
    void theThankYouEmailFitsEachKindOfPayment() {
        String balance = engine.process("mail/payment-thank-you", thanksContext("RENTAL_BALANCE", false));
        String settlement = engine.process("mail/payment-thank-you", thanksContext("SETTLEMENT", true));

        assertTrue(balance.contains("phần tiền thuê còn lại"), balance);
        assertTrue(balance.contains("500.000 đ"), balance);
        assertTrue(balance.contains("Tiền mặt"), balance);
        assertFalse(balance.contains("Đơn thuê đã được đóng"), balance);
        assertTrue(settlement.contains("hoàn tất quyết toán"), settlement);
        assertTrue(settlement.contains("Đơn thuê đã được đóng"), settlement);
    }

    private Context feeContext(int photoCount) {
        Context context = new Context(Locale.of("vi", "VN"));
        context.setVariable("customerName", "Nguyễn Văn An");
        context.setVariable("bookingId", 42L);
        context.setVariable("issueType", "Phạt nguội");
        context.setVariable("subject", "Vượt đèn đỏ");
        context.setVariable("occurredAt", OffsetDateTime.parse("2026-10-20T14:30:00+07:00"));
        context.setVariable("amount", new BigDecimal("800000"));
        context.setVariable("bookingUrl", "http://localhost:8081/bookings/42");
        context.setVariable("photoCount", photoCount);
        return context;
    }

    private Context fineContext(boolean attached) {
        Context context = new Context(Locale.of("vi", "VN"));
        context.setVariable("notice", new com.crms.mvc.dto.TrafficFineNoticeDTO(
                1L, 42L, "Nguyễn Văn An", "an@example.com", "51A-123.45",
                OffsetDateTime.parse("2026-10-20T14:30:00+07:00"), OffsetDateTime.parse("2026-10-21T10:00:00+07:00"), "Staff",
                OffsetDateTime.parse("2026-10-20T09:00:00+07:00"), OffsetDateTime.parse("2026-10-21T18:00:00+07:00"), "CTR-0001"));
        context.setVariable("bookingUrl", "http://localhost:8081/bookings/42");
        context.setVariable("hasAttachment", attached);
        context.setVariable("attachmentName", attached ? "bien-ban-phat-nguoi.jpg" : null);
        return context;
    }

    private Context thanksContext(String kind, boolean closed) {
        Context context = new Context(Locale.of("vi", "VN"));
        context.setVariable("customerName", "Nguyễn Văn An");
        context.setVariable("bookingId", 42L);
        context.setVariable("vehicleName", "Toyota Vios");
        context.setVariable("amount", new BigDecimal("500000"));
        context.setVariable("paymentMethod", "Tiền mặt");
        context.setVariable("paidAt", OffsetDateTime.parse("2026-11-08T14:30:00+07:00"));
        context.setVariable("kind", kind);
        context.setVariable("bookingClosed", closed);
        context.setVariable("bookingUrl", "http://localhost:8081/bookings/42");
        return context;
    }
}
