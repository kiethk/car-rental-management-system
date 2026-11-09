package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.notification.CustomerMailer;
import com.crms.mvc.service.ContractMailService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends the contract by email (Gmail App Password in application-local.properties), through {@link CustomerMailer}.
 * Email is optional: when no account is configured, sending fails with a clear message
 * and the caller keeps the booking and the PDF.
 */
@Service
public class SmtpContractMailService implements ContractMailService {
    private static final String TEMPLATE = "mail/contract-issued";
    static final String NOT_CONFIGURED_MESSAGE = "Chưa cấu hình email: thêm spring.mail.username và "
            + "spring.mail.password (mật khẩu ứng dụng Gmail) vào application-local.properties để gửi hợp đồng qua email.";

    private final CustomerMailer mailer;

    public SmtpContractMailService(CustomerMailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void sendContract(ContractDocumentDTO document, byte[] pdf) {
        if (!mailer.isConfigured()) {
            throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
        }
        try {
            Map<String, Resource> attachments = Map.of(document.getContractNumber() + ".pdf", new ByteArrayResource(pdf));
            mailer.send(document.getCustomerEmail(), "DriveNow - Hợp đồng thuê xe " + document.getContractNumber(),
                    TEMPLATE, Map.<String, Object>of("doc", document), attachments);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo email hợp đồng " + document.getContractNumber(), exception);
        }
    }
}
