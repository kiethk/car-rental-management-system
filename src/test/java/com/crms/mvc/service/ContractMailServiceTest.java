package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDocumentDTO;
import com.crms.mvc.notification.CustomerMailer;
import com.crms.mvc.service.impl.SmtpContractMailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractMailServiceTest {
    private final CustomerMailer mailer = mock(CustomerMailer.class);
    private final ContractMailService service = new SmtpContractMailService(mailer);

    @Test
    void missingGmailAccountGivesClearMessageInsteadOfSending() throws Exception {
        when(mailer.isConfigured()).thenReturn(false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.sendContract(new ContractDocumentDTO(), new byte[]{1}));

        assertTrue(error.getMessage().startsWith("Chưa cấu hình email"));
        verify(mailer, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void theContractGoesToTheCustomerAsAPdfNamedAfterItsNumber() throws Exception {
        when(mailer.isConfigured()).thenReturn(true);
        ContractDocumentDTO doc = new ContractDocumentDTO();
        doc.setContractNumber("CTR-20261108-0001");
        doc.setCustomerEmail("an@example.com");

        service.sendContract(doc, new byte[]{1, 2, 3});

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Resource>> attachments = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/contract-issued"), vars.capture(), attachments.capture());
        assertEquals("DriveNow - Hợp đồng thuê xe CTR-20261108-0001", subject.getValue());
        assertSame(doc, vars.getValue().get("doc"));
        assertEquals(java.util.Set.of("CTR-20261108-0001.pdf"), attachments.getValue().keySet());
    }

    @Test
    void aSendFailureIsReportedWithTheContractNumber() throws Exception {
        when(mailer.isConfigured()).thenReturn(true);
        doThrow(new IllegalStateException("smtp down")).when(mailer).send(any(), any(), any(), any(), any());
        ContractDocumentDTO doc = new ContractDocumentDTO();
        doc.setContractNumber("CTR-1");
        doc.setCustomerEmail("an@example.com");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.sendContract(doc, new byte[]{1}));

        assertTrue(error.getMessage().contains("CTR-1"));
    }
}
