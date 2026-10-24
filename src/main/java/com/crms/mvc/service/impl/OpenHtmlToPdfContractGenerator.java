package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.service.ContractDocumentGenerator;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Renders templates/pdf/rental-contract.html with Thymeleaf, then converts the XHTML to PDF with openhtmltopdf.
 */
@Service
public class OpenHtmlToPdfContractGenerator implements ContractDocumentGenerator {
    private static final String TEMPLATE = "pdf/rental-contract";
    // Must match font-family in the template. The embedded TTF is what makes Vietnamese diacritics render.
    private static final String FONT_FAMILY = "ContractFont";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ITemplateEngine templateEngine;
    private final Resource regularFont;
    private final Resource boldFont;

    public OpenHtmlToPdfContractGenerator(ITemplateEngine templateEngine,
                                          @Value("${crms.contract.font.regular}") Resource regularFont,
                                          @Value("${crms.contract.font.bold}") Resource boldFont) {
        this.templateEngine = templateEngine;
        this.regularFont = regularFont;
        this.boldFont = boldFont;
    }

    @Override
    public byte[] generatePdf(ContractDocumentDTO document) {
        Context context = new Context(VIETNAMESE);
        context.setVariable("doc", document);
        String xhtml = templateEngine.process(TEMPLATE, context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> open(regularFont), FONT_FAMILY, 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> open(boldFont), FONT_FAMILY, 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(xhtml, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render contract PDF " + document.getContractNumber(), exception);
        }
    }

    private InputStream open(Resource font) {
        try {
            return font.getInputStream();
        } catch (IOException exception) {
            throw new UncheckedIOException("Contract font not found: " + font.getDescription(), exception);
        }
    }
}
