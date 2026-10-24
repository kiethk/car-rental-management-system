package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import org.springframework.core.io.Resource;

import java.util.Optional;

public interface ContractService {
    /** Records the clickwrap acceptance for a new booking. Called inside the booking transaction. */
    void createContract(Booking booking, Long templateId, String acceptedIp);

    Optional<ContractDTO> findByBookingId(Long bookingId);

    /** Terms the booking was accepted under; fees are always computed from these, never the current version. */
    ContractTemplateDTO getTermsOfBooking(Long bookingId);

    /** Generates the PDF if missing and emails it if not yet sent. Safe to call more than once. */
    void issueContract(Long bookingId);

    Resource loadContractPdf(Long bookingId);

    Resource loadContractPdfForCustomer(Long bookingId, Long customerId);
}
