package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Contract;
import com.crms.mvc.repository.ContractRepository;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final PolicyService policyService;

    public ContractServiceImpl(ContractRepository contractRepository, PolicyService policyService) {
        this.contractRepository = contractRepository;
        this.policyService = policyService;
    }

    @Override
    public Contract createContractForBooking(Booking booking) {
        return contractRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            Contract contract = new Contract();
            String datePart = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            contract.setContractNumber("CTR-" + datePart + "-" + String.format("%04d", booking.getId()));
            contract.setBooking(booking);
            contract.setContractStatus("DRAFT");
            contract.setTermsSnapshot(policyService.buildTermsSnapshot());
            contract.setCreatedAt(OffsetDateTime.now());
            return contractRepository.save(contract);
        });
    }

    @Override
    public Contract signContract(Long bookingId, String signatureType) {
        Contract contract = contractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Contract not found for booking: " + bookingId));
        contract.setSignedAt(OffsetDateTime.now());
        contract.setSignatureType(signatureType != null ? signatureType : "DIGITAL_CHECK");
        contract.setContractStatus("SIGNED");
        return contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contract> getContractByBookingId(Long bookingId) {
        return contractRepository.findByBookingId(bookingId);
    }

    @Override
    public void updateContractStatus(Long bookingId, String status) {
        contractRepository.findByBookingId(bookingId).ifPresent(c -> {
            c.setContractStatus(status);
            contractRepository.save(c);
        });
    }
}
