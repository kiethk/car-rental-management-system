package com.crms.mvc.service.impl;

import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.CustomerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean register(CustomerRegisterDTO form) {
        String email = form.getEmail().trim();

        if (customerRepository.findByEmailIgnoreCase(email) != null
                || employeeRepository.findByEmailIgnoreCase(email) != null) {
            return false;
        }

        customerRepository.save(new Customer(
                form.getName().trim(),
                form.getPhone().trim(),
                email,
                passwordEncoder.encode(form.getPassword()),
                OffsetDateTime.now()
        ));
        return true;
    }
}
