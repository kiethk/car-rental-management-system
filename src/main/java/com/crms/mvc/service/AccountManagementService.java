package com.crms.mvc.service;

import com.crms.mvc.dto.AccountQueryDTO;
import com.crms.mvc.dto.AccountSummaryDTO;
import com.crms.mvc.dto.EmployeeCreateDTO;
import com.crms.mvc.dto.SessionAccount;
import org.springframework.data.domain.Page;

public interface AccountManagementService {
    Page<AccountSummaryDTO> search(AccountQueryDTO query);

    void setActive(String accountType, Long accountId, boolean active, SessionAccount actor);

    void createEmployee(EmployeeCreateDTO form);
}
