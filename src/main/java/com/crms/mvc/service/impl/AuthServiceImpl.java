package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(CustomerRepository customerRepository,
                           EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginIdentityDTO login(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        String trimmedEmail = email.trim();

        Customer customer = customerRepository.findByEmailIgnoreCase(trimmedEmail);
        Employee employee = employeeRepository.findByEmailIgnoreCase(trimmedEmail);

        if (customer != null && employee != null) {
            return null;
        }

        if (customer != null && passwordEncoder.matches(password, customer.getPasswordHash())) {
            return new LoginIdentityDTO("customer", customer.getId(), customer.getName(), null);
        }

        if (employee != null && passwordEncoder.matches(password, employee.getPasswordHash())) {
            return new LoginIdentityDTO("employee", employee.getId(), employee.getName(), employee.getRole());
        }

        return null;
    }
}
