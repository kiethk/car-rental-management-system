package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.FeeType;
import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueEvidence;
import com.crms.mvc.enums.IssueStatus;
import com.crms.mvc.enums.IssueType;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.event.IssueUpdatedEvent;
import com.crms.mvc.repository.BookingFeeRepository;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.IssueEvidenceRepository;
import com.crms.mvc.repository.IssueRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.service.impl.IssueServiceImpl;
import com.crms.mvc.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Photos are added while the Issue is recorded, and an edit made after the customer was told is announced as an
 * update that says what it changed.
 */
class IssuePhotosAndUpdateNoticeTest {
    private IssueRepository issueRepository;
    private BookingRepository bookingRepository;
    private BookingFeeRepository bookingFeeRepository;
    private IssueEvidenceRepository evidenceRepository;
    private FileUploadService fileUploadService;
    private ApplicationEventPublisher eventPublisher;
    private IssueService service;

    private final IssueActor staff = new IssueActor(2L, "staff");

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        bookingRepository = mock(BookingRepository.class);
        bookingFeeRepository = mock(BookingFeeRepository.class);
        evidenceRepository = mock(IssueEvidenceRepository.class);
        fileUploadService = mock(FileUploadService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        service = new IssueServiceImpl(issueRepository, bookingRepository, employeeRepository, bookingFeeRepository,
                evidenceRepository, fileUploadService, mock(LicensePlateRepository.class), mock(com.crms.mvc.repository.PaymentRepository.class), eventPublisher);

        Employee employee = new Employee();
        employee.setId(2L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingFeeRepository.save(any(BookingFee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- photos at creation ----

    @Test
    void photosGivenWithTheFormAreStoredAgainstTheNewIssue() {
        Booking booking = activeBooking();
        IssueFormDTO form = form("200000");
        form.setImages(List.of(photo("front.jpg"), photo("rear.jpg")));
        when(fileUploadService.saveIssueEvidence(any())).thenReturn("issue-a.jpg", "issue-b.jpg");

        Issue issue = service.create(form, staff);

        ArgumentCaptor<IssueEvidence> saved = ArgumentCaptor.forClass(IssueEvidence.class);
        verify(evidenceRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(List.of("issue-a.jpg", "issue-b.jpg"),
                saved.getAllValues().stream().map(IssueEvidence::getFileName).toList());
        assertEquals(issue, saved.getAllValues().get(0).getIssue());
        assertEquals(booking, issue.getBooking());
    }

    @Test
    void emptyFileInputsAreIgnored() {
        activeBooking();
        IssueFormDTO form = form("200000");
        form.setImages(List.of(new MockMultipartFile("images", new byte[0])));

        service.create(form, staff);

        verify(fileUploadService, never()).saveIssueEvidence(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void moreThanTenPhotosAreRefusedBeforeAnythingIsWritten() {
        activeBooking();
        IssueFormDTO form = form("200000");
        List<MultipartFile> eleven = new ArrayList<>();
        for (int i = 0; i < 11; i++) eleven.add(photo("p" + i + ".jpg"));
        form.setImages(eleven);

        assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));

        verify(fileUploadService, never()).saveIssueEvidence(any());
    }

    @Test
    void whenOnePhotoIsRefusedTheOnesAlreadyStoredAreRemoved() {
        activeBooking();
        IssueFormDTO form = form("200000");
        form.setImages(List.of(photo("ok.jpg"), photo("bad.gif")));
        when(fileUploadService.saveIssueEvidence(any()))
                .thenReturn("issue-ok.jpg")
                .thenThrow(new IllegalArgumentException("Chỉ chấp nhận định dạng ảnh JPEG, PNG hoặc WebP"));

        assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));

        verify(fileUploadService).deleteIssueEvidence("issue-ok.jpg");
    }

    @Test
    void theCustomerIsEmailedAtOnceWhenTheIssueIsRecorded() {
        activeBooking();

        Issue issue = service.create(form("200000"), staff);

        org.junit.jupiter.api.Assertions.assertNotNull(issue.getNotifiedAt());
        verify(eventPublisher).publishEvent(new com.crms.mvc.event.IssueFeeDueEvent(issue.getId()));
    }

    // ---- update notice ----

    @Test
    void editingAnAnnouncedIssuePublishesWhatItSaidBefore() {
        Issue issue = announcedIssue("200000");

        IssueFormDTO edit = form("350000");
        edit.setSubject("Dented bumper");
        service.update(7L, edit);

        ArgumentCaptor<IssueUpdatedEvent> event = ArgumentCaptor.forClass(IssueUpdatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(7L, event.getValue().issueId());
        assertEquals("Scratch on the bumper", event.getValue().previousSubject());
        assertEquals(new BigDecimal("200000"), event.getValue().previousFeeAmount());
        assertEquals("Dented bumper", issue.getSubject());
    }

    @Test
    void editingAnIssueWithoutChangingAnythingSendsNothing() {
        announcedIssue("200000");

        service.update(7L, form("200000"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void editingAnIssueTheCustomerWasNeverToldAboutSendsNothing() {
        Issue issue = announcedIssue("200000");
        issue.setNotifiedAt(null);

        service.update(7L, form("350000"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private Booking activeBooking() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setVehicle(vehicle);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        return booking;
    }

    /** An open Issue whose customer was already emailed, recorded exactly as {@link #form(String)} describes it. */
    private Issue announcedIssue(String fee) {
        Booking booking = activeBooking();
        IssueFormDTO original = form(fee);
        Issue issue = new Issue();
        issue.setId(7L);
        issue.setBooking(booking);
        issue.setIssueType(original.getIssueType());
        issue.setSubject(original.getSubject());
        issue.setDescription(original.getDescription());
        issue.setOccurredAt(original.getOccurredAt().atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime());
        issue.setStatus(IssueStatus.OPEN);
        issue.setNotifiedAt(OffsetDateTime.now().minusMinutes(5));
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));

        BookingFee existing = new BookingFee();
        existing.setBooking(booking);
        existing.setIssue(issue);
        existing.setFeeType(FeeType.ISSUE_COMPENSATION);
        existing.setAmount(new BigDecimal(fee));
        when(bookingFeeRepository.findByIssueId(7L)).thenReturn(List.of(existing));
        return issue;
    }

    private static final LocalDateTime OCCURRED = LocalDateTime.now(DateTimeUtils.VIETNAM_ZONE).minusHours(2).withNano(0);

    private IssueFormDTO form(String fee) {
        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(42L);
        form.setIssueType(IssueType.DAMAGE);
        form.setSubject("Scratch on the bumper");
        form.setDescription("About 15 cm.");
        form.setOccurredAt(OCCURRED);
        form.setFeeAmount(new BigDecimal(fee));
        return form;
    }

    private static MultipartFile photo(String name) {
        return new MockMultipartFile("images", name, "image/jpeg", new byte[]{1, 2, 3});
    }
}
