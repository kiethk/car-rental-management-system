package com.crms.mvc.support;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.service.SessionAccountService;

import java.util.Optional;

public class PassThroughSessionAccountService implements SessionAccountService {
    @Override
    public SessionAccount create(LoginIdentityDTO identity) {
        return new SessionAccount(identity.accountId(), identity.accountType(), identity.name(),
                null, identity.role(), TestSessions.STAMP);
    }

    @Override
    public Optional<SessionAccount> refresh(SessionAccount account) {
        return Optional.of(account);
    }

    @Override
    public SessionAccount fromCustomer(Customer customer) {
        return new SessionAccount(customer.getId(), SessionAccount.CUSTOMER, customer.getName(),
                customer.getEmail(), null, TestSessions.STAMP);
    }

    @Override
    public SessionAccount fromEmployee(Employee employee) {
        return new SessionAccount(employee.getId(), SessionAccount.EMPLOYEE, employee.getName(),
                employee.getEmail(), EnumTokens.of(employee.getRole()), TestSessions.STAMP);
    }
}
