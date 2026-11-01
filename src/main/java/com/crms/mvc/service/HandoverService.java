package com.crms.mvc.service;

import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.HandoverSummaryDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.entity.Employee;
import org.springframework.core.io.Resource;

import java.util.List;

public interface HandoverService {
    void checkIn(Long bookingId, Long employeeId, CheckInDTO form);

    void checkOut(Long bookingId, Long employeeId, CheckOutDTO form);

    void settle(Long bookingId, Long employeeId, SettlementDTO form);

    void reassignStaff(Long bookingId, Long staffId);

    List<Employee> findAssignableEmployees();

    HandoverSummaryDTO getSummary(Long bookingId);

    Resource loadEvidence(Long bookingId, Long issueId, boolean secondary);
}
