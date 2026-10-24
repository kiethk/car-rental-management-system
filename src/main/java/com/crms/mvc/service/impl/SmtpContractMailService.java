package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.service.ContractMailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
public class SmtpContractMailService implements ContractMailService {
    private static final String TEMPLATE = "mail/contract-issued";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String fromAddress;

    public SmtpContractMailService(JavaMailSender mailSender,
                                   ITemplateEngine templateEngine,
                                   @Value("${crms.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendContract(ContractDocumentDTO document, byte[] pdf) {
        Context context = new Context(VIETNAMESE);
        context.setVariable("doc", document);
        String body = templateEngine.process(TEMPLATE, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(document.getCustomerEmail());
            helper.setSubject("DriveNow - Hợp đồng thuê xe " + document.getContractNumber());
            helper.setText(body, true);
            helper.addAttachment(document.getContractNumber() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not build contract email " + document.getContractNumber(), exception);
        }
    }
}
