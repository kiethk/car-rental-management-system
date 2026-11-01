package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueEvidence;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.Payment;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Photos are private incident evidence: at most 10 per issue, editable while the issue is open and the fee has
 * not been locked, served only behind authenticated endpoints.
 */
class IssueEvidenceTest {
    private IssueRepository issueRepository;
    private IssueEvidenceRepository evidenceRepository;
    private FileUploadService fileUploadService;
    private BookingFeeRepository bookingFeeRepository;
    private IssueService service;

    private final MockMultipartFile photo = new MockMultipartFile("image", "dent.jpg", "image/jpeg", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        evidenceRepository = mock(IssueEvidenceRepository.class);
        fileUploadService = mock(FileUploadService.class);
        bookingFeeRepository = mock(BookingFeeRepository.class);
        service = new IssueServiceImpl(issueRepository, mock(BookingRepository.class), mock(EmployeeRepository.class),
                bookingFeeRepository, evidenceRepository, fileUploadService, mock(LicensePlateRepository.class),
                mock(ApplicationEventPublisher.class));

        when(fileUploadService.saveIssueEvidence(photo)).thenReturn("issue-abc.jpg");
        when(evidenceRepository.save(any(IssueEvidence.class))).thenAnswer(inv -> {
            IssueEvidence e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });
    }

    // ---- adding ----

    @Test
    void anEvidencePhotoCanBeAddedToAnOpenIssue() {
        Issue issue = issue(IssueStatus.OPEN);

        IssueEvidence saved = service.addEvidence(7L, photo);

        assertNotNull(saved);
        assertEquals("issue-abc.jpg", saved.getFileName());
        assertSame(issue, saved.getIssue());
        assertNotNull(saved.getUploadedAt());
        verify(evidenceRepository).save(any(IssueEvidence.class));
    }

    @Test
    void photosCannotBeAddedToAResolvedIssue() {
        issue(IssueStatus.RESOLVED);

        assertThrows(IllegalStateException.class, () -> service.addEvidence(7L, photo));

        verify(fileUploadService, never()).saveIssueEvidence(any());
    }

    @Test
    void photosCannotBeAddedToACancelledIssue() {
        issue(IssueStatus.CANCELLED);

        assertThrows(IllegalStateException.class, () -> service.addEvidence(7L, photo));

        verify(fileUploadService, never()).saveIssueEvidence(any());
    }

    @Test
    void onceAPaymentHasTakenTheFeeNoMorePhotosCanBeAdded() {
        for (String paymentStatus : List.of("pending", "paid")) {
            Issue issue = issue(IssueStatus.RESOLVED);
            feeTakenBy(issue, paymentStatus);

            assertThrows(IllegalStateException.class, () -> service.addEvidence(7L, photo), paymentStatus);
        }
        verify(fileUploadService, never()).saveIssueEvidence(any());
    }

    @Test
    void anIssueCannotHaveMoreThanTenPhotos() {
        issue(IssueStatus.OPEN);
        when(evidenceRepository.countByIssueId(7L)).thenReturn(10L);

        assertThrows(IllegalStateException.class, () -> service.addEvidence(7L, photo));

        verify(fileUploadService, never()).saveIssueEvidence(any());
    }

    @Test
    void emptyOrDisallowedUploadsAreRejectedByTheStorageService() {
        issue(IssueStatus.OPEN);
        when(fileUploadService.saveIssueEvidence(photo)).thenThrow(new IllegalArgumentException("invalid file"));

        assertThrows(IllegalArgumentException.class, () -> service.addEvidence(7L, photo));

        verify(evidenceRepository, never()).save(any());
    }

    // ---- removing ----

    @Test
    void anEvidencePhotoCanBeRemovedFromAnOpenIssue() {
        Issue issue = issue(IssueStatus.OPEN);
        IssueEvidence evidence = evidenceOf(issue);

        service.removeEvidence(7L, 3L);

        verify(evidenceRepository).delete(evidence);
        verify(fileUploadService).deleteIssueEvidence("issue-abc.jpg");
    }

    @Test
    void photosCannotBeRemovedOnceTheIssueIsResolvedOrCancelled() {
        for (IssueStatus status : List.of(IssueStatus.RESOLVED, IssueStatus.CANCELLED)) {
            Issue issue = issue(status);
            evidenceOf(issue);

            assertThrows(IllegalStateException.class, () -> service.removeEvidence(7L, 3L), status.name());
        }
        verify(evidenceRepository, never()).delete(any());
        verify(fileUploadService, never()).deleteIssueEvidence(any());
    }

    @Test
    void removingANonExistentPhotoFails() {
        issue(IssueStatus.OPEN);
        when(evidenceRepository.findByIdAndIssueId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.removeEvidence(7L, 99L));
    }

    // ---- reading ----

    @Test
    void staffCanReadAnyEvidencePhoto() {
        Issue issue = issue(IssueStatus.OPEN);
        evidenceOf(issue);
        Resource file = new ByteArrayResource(new byte[]{1});
        when(fileUploadService.loadIssueEvidence("issue-abc.jpg")).thenReturn(file);

        assertSame(file, service.loadEvidence(7L, 3L));
    }

    @Test
    void aCustomerCanReadPhotosOfTheirOwnBookingOnly() {
        Issue issue = issue(IssueStatus.OPEN);
        evidenceOf(issue);
        Resource file = new ByteArrayResource(new byte[]{1});
        when(fileUploadService.loadIssueEvidence("issue-abc.jpg")).thenReturn(file);

        assertSame(file, service.loadEvidenceForCustomer(7L, 3L, 5L));
        assertThrows(NoSuchElementException.class, () -> service.loadEvidenceForCustomer(7L, 3L, 6L));
    }

    @Test
    void aCustomerCannotReadPhotosOfACancelledIssue() {
        Issue issue = issue(IssueStatus.CANCELLED);
        evidenceOf(issue);
        Resource file = new ByteArrayResource(new byte[]{1});
        when(fileUploadService.loadIssueEvidence("issue-abc.jpg")).thenReturn(file);

        assertThrows(NoSuchElementException.class, () -> service.loadEvidenceForCustomer(7L, 3L, 5L));
    }

    private Issue issue(IssueStatus status) {
        Customer customer = new Customer();
        customer.setId(5L);
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        Issue issue = new Issue();
        issue.setId(7L);
        issue.setBooking(booking);
        issue.setStatus(status);
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(bookingFeeRepository.findByIssueId(7L)).thenReturn(List.of());
        when(evidenceRepository.countByIssueId(7L)).thenReturn(0L);
        return issue;
    }

    private void feeTakenBy(Issue issue, String paymentStatus) {
        Payment payment = new Payment();
        payment.setStatus(paymentStatus);
        BookingFee fee = new BookingFee();
        fee.setIssue(issue);
        fee.setAmount(new BigDecimal("1000"));
        fee.setPayment(payment);
        when(bookingFeeRepository.findByIssueId(7L)).thenReturn(List.of(fee));
    }

    private IssueEvidence evidenceOf(Issue issue) {
        IssueEvidence evidence = new IssueEvidence();
        evidence.setId(3L);
        evidence.setIssue(issue);
        evidence.setFileName("issue-abc.jpg");
        when(evidenceRepository.findByIdAndIssueId(3L, 7L)).thenReturn(Optional.of(evidence));
        return evidence;
    }
}
