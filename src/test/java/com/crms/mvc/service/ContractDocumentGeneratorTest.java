package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.service.impl.OpenHtmlToPdfContractGenerator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Renders the real templates/pdf/rental-contract.html with the bundled fonts and reads the text back,
 * which proves the XHTML is valid and Vietnamese diacritics survive into the PDF.
 */
class ContractDocumentGeneratorTest {
    private ContractDocumentGenerator generator;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        generator = new OpenHtmlToPdfContractGenerator(engine,
                new ClassPathResource("fonts/NotoSans-Regular.ttf"),
                new ClassPathResource("fonts/NotoSans-Bold.ttf"));
    }

    @Test
    void rendersVietnameseContractWithTermsFromTemplateVersion() throws Exception {
        byte[] pdf = generator.generatePdf(sampleDocument());

        assertTrue(pdf.length > 1000);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM"), text);
            assertTrue(text.contains("Nguyễn Thị Hồng Nhung"));
            assertTrue(text.contains("CTR-20261023-0042"));
            assertTrue(text.contains("Tiền đặt cọc (40%)"), "deposit percent comes from the template version");
            assertTrue(text.contains("ít nhất 48 giờ"), "cancellation window comes from the template version");
            assertTrue(text.contains("1.350.000 đ"), "amounts use Vietnamese number format");
            assertTrue(text.contains("Không hút thuốc trong xe"), "admin-written wording is printed");
        }
    }

    @Test
    void escapesAdminTextInsteadOfBreakingTheXml() {
        ContractDocumentDTO doc = sampleDocument();
        doc.getTerms().setTermsContent("Phí < 100.000 đ & không hoàn lại <script>");

        assertDoesNotThrow(() -> generator.generatePdf(doc));
    }

    private ContractDocumentDTO sampleDocument() {
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(2L);
        terms.setVersionNo(2);
        terms.setTitle("Hợp đồng thuê xe ô tô tự lái");
        terms.setTermsContent("1. Không hút thuốc trong xe.\n2. Trả xe đúng hẹn.");
        terms.setDepositRate(new BigDecimal("0.4000"));
        terms.setLateFeeMultiplier(new BigDecimal("1.50"));
        terms.setFreeCancellationHours(48);
        terms.setStatus(ContractTemplateStatus.ACTIVE);

        OffsetDateTime now = OffsetDateTime.of(2026, 10, 23, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        ContractDocumentDTO doc = new ContractDocumentDTO();
        doc.setContractNumber("CTR-20261023-0042");
        doc.setBookingId(42L);
        doc.setIssuedAt(now.plusMinutes(5));
        doc.setCustomerName("Nguyễn Thị Hồng Nhung");
        doc.setCustomerPhone("0901234567");
        doc.setCustomerEmail("nhung@example.com");
        doc.setLicenseNumber("790123456789");
        doc.setLicenseClass("B");
        doc.setLicenseExpiry(LocalDate.of(2030, 1, 1));
        doc.setVehicleName("Toyota Vios");
        doc.setVehicleCategory("sedan");
        doc.setSeats(5);
        doc.setTransmission("automatic");
        doc.setFuelType("gasoline");
        doc.setPickupTime(now.plusDays(1));
        doc.setScheduledEndTime(now.plusDays(1).plusHours(9));
        doc.setRentalHours(9);
        doc.setPricePerHour(new BigDecimal("150000"));
        doc.setRentalPrice(new BigDecimal("1350000"));
        doc.setDiscountAmount(BigDecimal.ZERO);
        doc.setTotalPrice(new BigDecimal("1350000"));
        doc.setDepositAmount(new BigDecimal("540000"));
        doc.setRemainingAmount(new BigDecimal("810000"));
        doc.setDepositTransactionCode("TXN-DEP-42");
        doc.setDepositPaidAt(now.plusMinutes(4));
        doc.setAcceptedAt(now);
        doc.setAcceptedIp("127.0.0.1");
        doc.setTerms(terms);
        return doc;
    }
}
