package com.crms.mvc.service.impl;

import com.crms.mvc.dto.PasswordChangeDTO;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.EmployeeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean changePassword(Long employeeId, PasswordChangeDTO form) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || !passwordEncoder.matches(
                form.getCurrentPassword(), employee.getPasswordHash())) {
            return false;
        }

        employee.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        employeeRepository.save(employee);
        return true;
    }
}
