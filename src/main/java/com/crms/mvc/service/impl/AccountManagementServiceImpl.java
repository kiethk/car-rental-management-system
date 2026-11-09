package com.crms.mvc.service.impl;

import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.dto.AccountQueryDTO;
import com.crms.mvc.dto.AccountSummaryDTO;
import com.crms.mvc.dto.EmployeeCreateDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.AccountManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional
public class AccountManagementServiceImpl implements AccountManagementService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementServiceImpl(CustomerRepository customerRepository,
                                        EmployeeRepository employeeRepository,
                                        PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountSummaryDTO> search(AccountQueryDTO query) {
        Page<AccountSummaryDTO> page = load(query, query.getPageIndex());
        if (page.isEmpty() && page.getTotalPages() > 0) {
            return load(query, page.getTotalPages() - 1);
        }
        return page;
    }

    @Override
    public void setActive(String accountType, Long accountId, boolean active, SessionAccount actor) {
        if (SessionAccount.EMPLOYEE.equals(accountType)) {
            if (!active && actor.isEmployee() && accountId.equals(actor.getId())) {
                throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản của mình");
            }
            Employee employee = employeeRepository.findById(accountId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên: " + accountId));
            employee.setActive(active);
            employeeRepository.save(employee);
            return;
        }
        if (SessionAccount.CUSTOMER.equals(accountType)) {
            Customer customer = customerRepository.findById(accountId)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + accountId));
            customer.setActive(active);
            customerRepository.save(customer);
            return;
        }
        throw new IllegalArgumentException("Loại tài khoản không hợp lệ");
    }

    @Override
    public void createEmployee(EmployeeCreateDTO form) {
        String email = form.getEmail().trim();
        if (customerRepository.findByEmailIgnoreCase(email) != null
                || employeeRepository.findByEmailIgnoreCase(email) != null) {
            throw new IllegalArgumentException("Email này đã được sử dụng");
        }

        Employee employee = new Employee();
        employee.setName(form.getName().trim());
        employee.setPhone(form.getPhone().trim());
        employee.setEmail(email);
        employee.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        employee.setRole(EmployeeRole.from(form.getRole()));
        employeeRepository.save(employee);
    }

    private Page<AccountSummaryDTO> load(AccountQueryDTO query, int pageIndex) {
        Pageable pageable = PageRequest.of(pageIndex, query.getSize(), Sort.by("name", "id"));
        if (SessionAccount.EMPLOYEE.equals(query.getType())) {
            return employeeRepository.search(query.getLikePattern(), pageable)
                    .map(employee -> new AccountSummaryDTO(employee.getId(), SessionAccount.EMPLOYEE, employee.getName(),
                            employee.getEmail(), employee.getPhone(), EnumTokens.of(employee.getRole()), employee.isActive()));
        }
        return customerRepository.search(query.getLikePattern(), pageable)
                .map(customer -> new AccountSummaryDTO(customer.getId(), SessionAccount.CUSTOMER, customer.getName(),
                        customer.getEmail(), customer.getPhone(), null, customer.isActive()));
    }
}
