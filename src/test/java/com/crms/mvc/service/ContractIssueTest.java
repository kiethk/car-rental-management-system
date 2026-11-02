package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Contract;
import com.crms.mvc.repository.ContractRepository;
import com.crms.mvc.repository.ContractTemplateRepository;
import com.crms.mvc.repository.LicenseDocumentRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Issuing is guarded by a row lock, so whoever comes second finds the work done and sends nothing. */
class ContractIssueTest {
    private final ContractRepository contractRepository = mock(ContractRepository.class);
    private final ContractDocumentGenerator generator = mock(ContractDocumentGenerator.class);
    private final ContractMailService mailService = mock(ContractMailService.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private ContractService service;

    @BeforeEach
    void setUp() {
        service = new ContractServiceImpl(contractRepository, mock(ContractTemplateRepository.class),
                mock(LicenseDocumentRepository.class), mock(PaymentRepository.class),
                generator, mailService, fileUploadService);
    }

    @Test
    void aSecondIssueAfterTheFirstFinishedDoesNothing() {
        Contract contract = contract();
        contract.setIssuedAt(OffsetDateTime.now().minusSeconds(40));
        contract.setEmailedAt(OffsetDateTime.now());
        contract.setFileName("CTR-20261102-0004.pdf");
        when(contractRepository.findByBookingIdForUpdate(4L)).thenReturn(Optional.of(contract));

        service.issueContract(4L);

        verify(contractRepository).findByBookingIdForUpdate(4L);
        verify(contractRepository, never()).findByBookingId(any());
        verify(generator, never()).generatePdf(any());
        verify(fileUploadService, never()).saveContractPdf(any(), any());
        verify(mailService, never()).sendContract(any(), any());
    }

    private Contract contract() {
        Booking booking = new Booking();
        booking.setId(4L);
        booking.setStatus("confirmed");
        Contract contract = new Contract();
        contract.setContractNumber("CTR-20261102-0004");
        contract.setBooking(booking);
        return contract;
    }
}
