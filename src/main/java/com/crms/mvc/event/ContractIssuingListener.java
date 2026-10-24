package com.crms.mvc.event;

import com.crms.mvc.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Issues the contract PDF and emails it once the deposit transaction has committed.
 * AFTER_COMMIT: a PDF/email failure can never roll back the payment.
 * Async: the customer is redirected to the success page without waiting for the PDF or the SMTP server.
 */
@Component
public class ContractIssuingListener {
    private static final Logger log = LoggerFactory.getLogger(ContractIssuingListener.class);

    private final ContractService contractService;

    public ContractIssuingListener(ContractService contractService) {
        this.contractService = contractService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositConfirmed(DepositConfirmedEvent event) {
        try {
            contractService.issueContract(event.bookingId());
        } catch (RuntimeException exception) {
            log.error("Could not issue contract for booking {}", event.bookingId(), exception);
        }
    }
}
