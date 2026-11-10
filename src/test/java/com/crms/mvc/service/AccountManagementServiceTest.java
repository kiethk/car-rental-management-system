package com.crms.mvc.service;

import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.dto.EmployeeCreateDTO;
import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.exception.AccountLockedException;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.impl.AccountManagementServiceImpl;
import com.crms.mvc.service.impl.AuthServiceImpl;
import com.crms.mvc.service.impl.SessionAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountManagementServiceTest {
    private static final SessionAccount ADMIN =
            new SessionAccount(1L, SessionAccount.EMPLOYEE, "Admin", "admin@crms.test", "admin", "stamp");

    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AccountManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountManagementServiceImpl(customerRepository, employeeRepository, passwordEncoder);
    }

    @Test
    void adminCannotLockOwnAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setActive(SessionAccount.EMPLOYEE, 1L, false, ADMIN));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void customerWithSameIdAsAdminCanBeLocked() {
        Customer customer = customer();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        service.setActive(SessionAccount.CUSTOMER, 1L, false, ADMIN);

        assertFalse(customer.isActive());
    }

    @Test
    void unknownAccountTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.setActive("root", 5L, false, ADMIN));
    }

    @Test
    void createEmployeeEncodesPassword() {
        when(passwordEncoder.encode("temporary1")).thenReturn("hash");

        service.createEmployee(employeeForm());

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertEquals("hash", saved.getValue().getPasswordHash());
        assertEquals(EmployeeRole.STAFF, saved.getValue().getRole());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    void createEmployeeRejectsEmailUsedByCustomer() {
        when(customerRepository.findByEmailIgnoreCase("new@crms.test")).thenReturn(customer());

        assertThrows(IllegalArgumentException.class, () -> service.createEmployee(employeeForm()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void lockedAccountsCannotLogIn() {
        AuthServiceImpl auth = new AuthServiceImpl(customerRepository, employeeRepository, passwordEncoder);
        Customer customer = customer();
        when(customerRepository.findByEmailIgnoreCase("c@crms.test")).thenReturn(customer);
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        LoginIdentityDTO active = auth.login("c@crms.test", "password");
        customer.setActive(false);

        assertNotNull(active);
        assertThrows(AccountLockedException.class, () -> auth.login("c@crms.test", "password"));
    }

    @Test
    void lockedAccountSessionIsRevokedOnNextRequest() {
        SessionAccountServiceImpl sessions = new SessionAccountServiceImpl(customerRepository, employeeRepository);
        Customer customer = customer();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        SessionAccount account = sessions.fromCustomer(customer);

        assertTrue(sessions.refresh(account).isPresent());
        customer.setActive(false);
        assertTrue(sessions.refresh(account).isEmpty());
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Customer");
        customer.setEmail("c@crms.test");
        customer.setPasswordHash("hash");
        return customer;
    }

    private EmployeeCreateDTO employeeForm() {
        EmployeeCreateDTO form = new EmployeeCreateDTO();
        form.setName("New Staff");
        form.setPhone("0900000000");
        form.setEmail("new@crms.test");
        form.setPassword("temporary1");
        form.setRole("staff");
        return form;
    }
}
