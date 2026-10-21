package com.crms.mvc.service.impl;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final CustomerRepository customerRepository;
    public AuthServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    public Optional<Customer> login(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        return customerRepository.findByEmailIgnoreCase(email.trim())
                .filter(customer -> password.equals(customer.getPasswordHash()));
    }
}
