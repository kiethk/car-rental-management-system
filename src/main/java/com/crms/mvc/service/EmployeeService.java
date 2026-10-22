package com.crms.mvc.service;

import com.crms.mvc.dto.PasswordChangeDTO;

public interface EmployeeService {
    boolean changePassword(Long employeeId, PasswordChangeDTO form);
}
