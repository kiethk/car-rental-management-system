package com.crms.mvc.service;

import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
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

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Only a collision or damage that still stands (not cancelled) sends the returned car to maintenance. */
class IssueVehicleDamageTest {
    private IssueRepository issueRepository;
    private IssueService service;

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        service = new IssueServiceImpl(issueRepository, mock(BookingRepository.class), mock(EmployeeRepository.class),
                mock(BookingFeeRepository.class), mock(IssueEvidenceRepository.class), mock(FileUploadService.class),
                mock(LicensePlateRepository.class), mock(ApplicationEventPublisher.class));
    }

    @Test
    void aStandingCollisionOrDamageMeansTheCarIsDamaged() {
        when(issueRepository.existsByBookingIdAndIssueTypeInAndStatusNot(eq(42L), any(), eq(IssueStatus.CANCELLED)))
                .thenReturn(true);

        assertTrue(service.bookingHasVehicleDamage(42L));
    }

    @Test
    void trafficFinesAndOtherIssuesDoNotCountAsDamage() {
        service.bookingHasVehicleDamage(42L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<IssueType>> types = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(issueRepository).existsByBookingIdAndIssueTypeInAndStatusNot(eq(42L), types.capture(), eq(IssueStatus.CANCELLED));
        assertEquals(Set.of(IssueType.COLLISION, IssueType.DAMAGE), Set.copyOf(types.getValue()));
    }

    @Test
    void noMatchingIssueMeansTheCarIsFine() {
        when(issueRepository.existsByBookingIdAndIssueTypeInAndStatusNot(eq(42L), any(), eq(IssueStatus.CANCELLED)))
                .thenReturn(false);

        assertFalse(service.bookingHasVehicleDamage(42L));
    }
}
