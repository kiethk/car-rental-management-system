package com.crms.mvc.service.impl;

import com.crms.mvc.dto.CustomerProfileDTO;
import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.PasswordChangeDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.CustomerService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
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

    @Override
    public CustomerProfileDTO getProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return null;
        }

        CustomerProfileDTO profile = new CustomerProfileDTO();
        profile.setName(customer.getName());
        profile.setEmail(customer.getEmail());
        profile.setPhone(customer.getPhone());
        return profile;
    }

    @Override
    public boolean updateProfile(Long customerId, CustomerProfileDTO form) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return false;
        }

        customer.setPhone(form.getPhone().trim());
        customerRepository.save(customer);
        return true;
    }

    @Override
    public boolean changePassword(Long customerId, PasswordChangeDTO form) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null || !passwordEncoder.matches(
                form.getCurrentPassword(), customer.getPasswordHash())) {
            return false;
        }

        customer.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        customerRepository.save(customer);
        return true;
    }
}
