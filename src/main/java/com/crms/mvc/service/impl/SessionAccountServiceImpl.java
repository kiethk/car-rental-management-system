package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.SessionAccountService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class SessionAccountServiceImpl implements SessionAccountService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public SessionAccountServiceImpl(CustomerRepository customerRepository,
                                     EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public SessionAccount create(LoginIdentityDTO identity) {
        SessionAccount probe = new SessionAccount(identity.accountId(), identity.accountType(),
                identity.name(), null, identity.role(), null);
        return load(probe).orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
    }

    @Override
    public Optional<SessionAccount> refresh(SessionAccount account) {
        if (account.getId() == null || account.getCredentialStamp() == null) {
            return Optional.empty();
        }
        return load(account)
                .filter(fresh -> fresh.getCredentialStamp().equals(account.getCredentialStamp()));
    }

    @Override
    public SessionAccount fromCustomer(Customer customer) {
        return new SessionAccount(customer.getId(), SessionAccount.CUSTOMER, customer.getName(),
                customer.getEmail(), null, stampOf(customer.getPasswordHash()));
    }

    @Override
    public SessionAccount fromEmployee(Employee employee) {
        return new SessionAccount(employee.getId(), SessionAccount.EMPLOYEE, employee.getName(),
                employee.getEmail(), employee.getRole(), stampOf(employee.getPasswordHash()));
    }

    private Optional<SessionAccount> load(SessionAccount account) {
        if (account.isCustomer()) {
            return customerRepository.findById(account.getId()).filter(Customer::isActive).map(this::fromCustomer);
        }
        if (account.isEmployee()) {
            return employeeRepository.findById(account.getId())
                    .filter(Employee::isActive)
                    .map(this::fromEmployee)
                    .filter(SessionAccount::isStaffOrAdmin);
        }
        return Optional.empty();
    }

    private String stampOf(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
