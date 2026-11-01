package com.crms.mvc.service;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;

import java.util.Optional;

public interface SessionAccountService {
    SessionAccount create(LoginIdentityDTO identity);

    Optional<SessionAccount> refresh(SessionAccount account);

    SessionAccount fromCustomer(Customer customer);

    SessionAccount fromEmployee(Employee employee);
}
