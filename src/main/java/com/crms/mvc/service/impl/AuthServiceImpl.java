package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(CustomerRepository customerRepository,
                           EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public LoginIdentityDTO login(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        String trimmedEmail = email.trim();

        // In-code Admin Account (No DB seed required)
        if (("admin@crms.com".equalsIgnoreCase(trimmedEmail) || "admin@gmail.com".equalsIgnoreCase(trimmedEmail) || "admin".equalsIgnoreCase(trimmedEmail))
                && ("admin123".equals(password) || "123456".equals(password) || "admin".equals(password))) {
            Employee admin = employeeRepository.findByEmailIgnoreCase("admin@crms.com");
            if (admin == null) {
                try {
                    admin = new Employee();
                    admin.setName("System Admin");
                    admin.setEmail("admin@crms.com");
                    admin.setPhone("0988888888");
                    admin.setRole("admin");
                    admin.setPasswordHash(passwordEncoder.encode("admin123"));
                    admin = employeeRepository.save(admin);
                } catch (Exception ignored) {
                }
            }
            Long adminId = (admin != null && admin.getId() != null) ? admin.getId() : 1L;
            String adminName = (admin != null && admin.getName() != null) ? admin.getName() : "System Admin";
            return new LoginIdentityDTO("employee", adminId, adminName, "admin");
        }

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
