package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.service.impl.SmtpContractMailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ContractMailServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void missingGmailAccountGivesClearMessageInsteadOfSending() {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        ContractMailService service = new SmtpContractMailService(provider, mock(ITemplateEngine.class), "", "", "");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.sendContract(new ContractDocumentDTO(), new byte[]{1}));

        assertTrue(error.getMessage().startsWith("Email is not configured"));
        verify(sender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}
