package com.crms.mvc.service;

import com.crms.mvc.dto.CustomerRegisterDTO;

public interface CustomerService {
    boolean register(CustomerRegisterDTO form);
}
