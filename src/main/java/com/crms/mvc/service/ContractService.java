package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Contract;
import java.util.Optional;

public interface ContractService {
    Contract createContractForBooking(Booking booking);
    Contract signContract(Long bookingId, String signatureType);
    Optional<Contract> getContractByBookingId(Long bookingId);
    void updateContractStatus(Long bookingId, String status);
}
