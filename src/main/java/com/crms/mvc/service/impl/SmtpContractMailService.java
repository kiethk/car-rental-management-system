package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.service.ContractMailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Sends the contract through SMTP (Gmail App Password in application-local.properties).
 * Email is optional: when no account is configured, sending fails with a clear message
 * and the caller keeps the booking and the PDF.
 */
@Service
public class SmtpContractMailService implements ContractMailService {
    private static final String TEMPLATE = "mail/contract-issued";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");
    static final String NOT_CONFIGURED_MESSAGE = "Email is not configured: add spring.mail.username and "
            + "spring.mail.password (Gmail App Password) to application-local.properties to email contracts.";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final String username;
    private final String password;
    private final String fromAddress;

    public SmtpContractMailService(ObjectProvider<JavaMailSender> mailSender,
                                   ITemplateEngine templateEngine,
                                   @Value("${spring.mail.username:}") String username,
                                   @Value("${spring.mail.password:}") String password,
                                   @Value("${crms.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendContract(ContractDocumentDTO document, byte[] pdf) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
        }

        Context context = new Context(VIETNAMESE);
        context.setVariable("doc", document);
        String body = templateEngine.process(TEMPLATE, context);

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(document.getCustomerEmail());
            helper.setSubject("DriveNow - Hợp đồng thuê xe " + document.getContractNumber());
            helper.setText(body, true);
            helper.addAttachment(document.getContractNumber() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            sender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not build contract email " + document.getContractNumber(), exception);
        }
    }
}
