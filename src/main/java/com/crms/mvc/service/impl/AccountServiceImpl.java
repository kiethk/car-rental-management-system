package com.crms.mvc.service.impl;

import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.AccountService;
import com.crms.mvc.service.SessionAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class AccountServiceImpl implements AccountService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final SessionAccountService sessionAccountService;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(CustomerRepository customerRepository,
                              EmployeeRepository employeeRepository,
                              SessionAccountService sessionAccountService,
                              PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.sessionAccountService = sessionAccountService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountProfileDTO getProfile(SessionAccount account) {
        AccountProfileDTO profile = new AccountProfileDTO();
        if (account.isCustomer()) {
            Customer customer = findCustomer(account);
            profile.setName(customer.getName());
            profile.setEmail(customer.getEmail());
            profile.setPhone(customer.getPhone());
        } else {
            Employee employee = findEmployee(account);
            profile.setName(employee.getName());
            profile.setEmail(employee.getEmail());
            profile.setPhone(employee.getPhone());
        }
        return profile;
    }

    @Override
    @Transactional
    public SessionAccount update(SessionAccount account, AccountProfileDTO form) {
        if (account.isCustomer()) {
            Customer customer = findCustomer(account);
            customer.setPhone(form.getPhone().trim());
            if (form.isPasswordChangeRequested()) {
                customer.setPasswordHash(newPasswordHash(customer.getPasswordHash(), form));
            }
            return sessionAccountService.fromCustomer(customerRepository.save(customer));
        }

        Employee employee = findEmployee(account);
        employee.setPhone(form.getPhone().trim());
        if (form.isPasswordChangeRequested()) {
            employee.setPasswordHash(newPasswordHash(employee.getPasswordHash(), form));
        }
        return sessionAccountService.fromEmployee(employeeRepository.save(employee));
    }

    private String newPasswordHash(String currentHash, AccountProfileDTO form) {
        if (!passwordEncoder.matches(form.getCurrentPassword(), currentHash)) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        return passwordEncoder.encode(form.getNewPassword());
    }

    private Customer findCustomer(SessionAccount account) {
        return customerRepository.findById(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
    }

    private Employee findEmployee(SessionAccount account) {
        if (!account.isEmployee()) {
            throw new NoSuchElementException("Không tìm thấy tài khoản");
        }
        return employeeRepository.findById(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản"));
    }
}
