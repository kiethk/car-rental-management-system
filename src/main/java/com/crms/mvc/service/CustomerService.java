package com.crms.mvc.service;

import com.crms.mvc.dto.CustomerProfileDTO;
import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.PasswordChangeDTO;

public interface CustomerService {
    boolean register(CustomerRegisterDTO form);

    CustomerProfileDTO getProfile(Long customerId);

    boolean updateProfile(Long customerId, CustomerProfileDTO form);

    boolean changePassword(Long customerId, PasswordChangeDTO form);
}
