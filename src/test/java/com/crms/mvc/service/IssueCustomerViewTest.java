package com.crms.mvc.service;

import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueEvidenceRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.service.impl.IssueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A customer sees why they are being charged, but only on their own booking and never what was cancelled. */
class IssueCustomerViewTest {
    private BookingRepository bookingRepository;
    private IssueRepository issueRepository;
    private IssueService service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        issueRepository = mock(IssueRepository.class);
        service = new IssueServiceImpl(issueRepository, bookingRepository, mock(EmployeeRepository.class),
                mock(BookingFeeRepository.class), mock(IssueEvidenceRepository.class), mock(FileUploadService.class),
                mock(LicensePlateRepository.class), mock(com.crms.mvc.repository.PaymentRepository.class), mock(ApplicationEventPublisher.class));

        Customer owner = new Customer();
        owner.setId(5L);
        booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(owner);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
    }

    @Test
    void theOwnerSeesTheirIssuesWithoutTheCancelledOnes() {
        when(issueRepository.findByBookingIdWithDetails(42L)).thenReturn(List.of(
                issue(1L, IssueStatus.OPEN), issue(2L, IssueStatus.CANCELLED), issue(3L, IssueStatus.RESOLVED)));

        List<IssueDTO> visible = service.findByBookingForCustomer(42L, 5L);

        assertEquals(List.of(1L, 3L), visible.stream().map(IssueDTO::getId).toList());
    }

    @Test
    void anotherCustomerCannotSeeTheBookingsIssues() {
        assertThrows(NoSuchElementException.class, () -> service.findByBookingForCustomer(42L, 6L));
        assertThrows(NoSuchElementException.class, () -> service.findByBookingForCustomer(42L, null));

        verify(issueRepository, never()).findByBookingIdWithDetails(42L);
    }

    @Test
    void aBookingThatDoesNotExistIsNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.findByBookingForCustomer(99L, 5L));
    }

    private Issue issue(Long id, IssueStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        Employee reporter = new Employee();
        reporter.setId(2L);
        Issue issue = new Issue();
        issue.setId(id);
        issue.setBooking(booking);
        issue.setVehicle(vehicle);
        issue.setEmployee(reporter);
        issue.setIssueType(IssueType.DAMAGE);
        issue.setSubject("Scratch " + id);
        issue.setStatus(status);
        issue.setOccurredAt(OffsetDateTime.now());
        issue.setReportedAt(OffsetDateTime.now());
        return issue;
    }
}
