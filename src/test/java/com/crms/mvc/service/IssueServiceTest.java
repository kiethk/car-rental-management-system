package com.crms.mvc.service;

import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
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
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.service.impl.IssueServiceImpl;
import com.crms.mvc.util.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Recording and editing an Issue: who may be charged for what, and where the fee lives. */
class IssueServiceTest {
    private IssueRepository issueRepository;
    private BookingRepository bookingRepository;
    private EmployeeRepository employeeRepository;
    private BookingFeeRepository bookingFeeRepository;
    private IssueService service;

    private final IssueActor staff = new IssueActor(2L, EmployeeRole.STAFF);

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        bookingRepository = mock(BookingRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        bookingFeeRepository = mock(BookingFeeRepository.class);
        service = new IssueServiceImpl(issueRepository, bookingRepository, employeeRepository, bookingFeeRepository,
                mock(IssueEvidenceRepository.class), mock(FileUploadService.class), mock(LicensePlateRepository.class),
                mock(ApplicationEventPublisher.class));

        Employee employee = new Employee();
        employee.setId(2L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingFeeRepository.save(any(BookingFee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void anIssueTakesItsVehicleFromTheBookingAndStartsOpen() {
        Booking booking = bookingWithStatus(Booking.STATUS_ACTIVE);

        Issue issue = service.create(form(booking, "200000"), staff);

        assertSame(booking, issue.getBooking());
        assertSame(booking.getVehicle(), issue.getVehicle());
        assertEquals(2L, issue.getEmployee().getId());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertEquals(IssueType.DAMAGE, issue.getIssueType());
        assertNotNull(issue.getReportedAt());
        assertNotNull(issue.getOccurredAt());
    }

    @Test
    void aFeeAboveZeroCreatesOneIssueCompensationFeeForTheBooking() {
        Booking booking = bookingWithStatus(Booking.STATUS_ACTIVE);

        Issue issue = service.create(form(booking, "200000"), staff);

        ArgumentCaptor<BookingFee> captor = ArgumentCaptor.forClass(BookingFee.class);
        verify(bookingFeeRepository).save(captor.capture());
        BookingFee fee = captor.getValue();
        assertEquals(BookingFee.FEE_TYPE_ISSUE_COMPENSATION, fee.getFeeType());
        assertEquals(new BigDecimal("200000"), fee.getAmount());
        assertSame(issue, fee.getIssue());
        assertSame(booking, fee.getBooking());
        assertNull(fee.getPayment());
    }

    @Test
    void feeMustBeStrictlyPositive() {
        Booking booking = bookingWithStatus(Booking.STATUS_ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> service.create(form(booking, null), staff));
        assertThrows(IllegalArgumentException.class, () -> service.create(form(booking, "0"), staff));
        assertThrows(IllegalArgumentException.class, () -> service.create(form(booking, "-50000"), staff));

        verify(bookingFeeRepository, never()).save(any());
    }

    @Test
    void issuesCanBeRecordedOnlyWhileTheRentalIsOpen() {
        for (String status : List.of("pending_payment", "confirmed", "cancelled", "completed")) {
            Booking booking = bookingWithStatus(status);
            assertThrows(IllegalStateException.class, () -> service.create(form(booking, "200000"), staff), status);
        }
        verify(issueRepository, never()).save(any());

        assertNotNull(service.create(form(bookingWithStatus(Booking.STATUS_ACTIVE), "200000"), staff));
    }

    @Test
    void editingAnOpenIssueChangesItsFields() {
        Issue issue = openIssueWithFee("100000");
        IssueFormDTO edit = form(issue.getBooking(), "100000");
        edit.setIssueType(IssueType.COLLISION);
        edit.setSubject("Rear-ended at a junction");
        edit.setDescription("Customer reported by phone.");

        service.update(issue.getId(), edit);

        assertEquals(IssueType.COLLISION, issue.getIssueType());
        assertEquals("Rear-ended at a junction", issue.getSubject());
        assertEquals("Customer reported by phone.", issue.getDescription());
    }

    @Test
    void changingTheFeeUpdatesTheSameFeeRow() {
        Issue issue = openIssueWithFee("100000");
        BookingFee existing = feeOf(issue);

        service.update(issue.getId(), form(issue.getBooking(), "350000"));

        assertEquals(new BigDecimal("350000"), existing.getAmount());
        verify(bookingFeeRepository).save(existing);
    }

    @Test
    void settingTheFeeToZeroOrNegativeThrows() {
        Issue issue = openIssueWithFee("100000");

        assertThrows(IllegalArgumentException.class, () -> service.update(issue.getId(), form(issue.getBooking(), "0")));
        assertThrows(IllegalArgumentException.class, () -> service.update(issue.getId(), form(issue.getBooking(), "-100")));
    }

    @Test
    void addingAFeeToAnIssueThatHadNoneCreatesIt() {
        Issue issue = openIssue();
        when(bookingFeeRepository.findByIssueId(issue.getId())).thenReturn(List.of());

        service.update(issue.getId(), form(issue.getBooking(), "50000"));

        verify(bookingFeeRepository).save(any(BookingFee.class));
    }

    @Test
    void anIssueThatIsNoLongerOpenCannotBeEdited() {
        Issue issue = openIssueWithFee("100000");
        issue.setStatus(IssueStatus.RESOLVED);

        assertThrows(IllegalStateException.class, () -> service.update(issue.getId(), form(issue.getBooking(), "1")));

        assertEquals(IssueType.DAMAGE, issue.getIssueType());
        verify(bookingFeeRepository, never()).save(any());
    }

    @Test
    void aNegativeFeeIsRejected() {
        Booking booking = bookingWithStatus(Booking.STATUS_ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> service.create(form(booking, "-5"), staff));

        verify(issueRepository, never()).save(any());
    }

    private Booking bookingWithStatus(String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setStatus(status);
        booking.setVehicle(vehicle);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        return booking;
    }

    private IssueFormDTO form(Booking booking, String fee) {
        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(booking.getId());
        form.setIssueType(IssueType.DAMAGE);
        form.setSubject("Scratch on the bumper");
        form.setDescription("About 15 cm.");
        form.setOccurredAt(LocalDateTime.now(DateTimeUtils.VIETNAM_ZONE).minusHours(2));
        form.setFeeAmount(fee == null ? null : new BigDecimal(fee));
        return form;
    }

    private Issue openIssue() {
        Booking booking = bookingWithStatus(Booking.STATUS_ACTIVE);
        Issue issue = new Issue();
        issue.setId(7L);
        issue.setBooking(booking);
        issue.setVehicle(booking.getVehicle());
        issue.setIssueType(IssueType.DAMAGE);
        issue.setSubject("Scratch on the bumper");
        issue.setStatus(IssueStatus.OPEN);
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        return issue;
    }

    private Issue openIssueWithFee(String amount) {
        Issue issue = openIssue();
        BookingFee fee = new BookingFee();
        fee.setBooking(issue.getBooking());
        fee.setIssue(issue);
        fee.setFeeType(BookingFee.FEE_TYPE_ISSUE_COMPENSATION);
        fee.setAmount(new BigDecimal(amount));
        when(bookingFeeRepository.findByIssueId(issue.getId())).thenReturn(List.of(fee));
        return issue;
    }

    private BookingFee feeOf(Issue issue) {
        return bookingFeeRepository.findByIssueId(issue.getId()).get(0);
    }
}
