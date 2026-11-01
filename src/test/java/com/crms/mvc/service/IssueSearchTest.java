package com.crms.mvc.service;

import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.dto.IssueStatsDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The admin list: newest first, never past the last page, and the plate shown is the one on the car back then. */
class IssueSearchTest {
    private IssueRepository issueRepository;
    private BookingFeeRepository feeRepository;
    private LicensePlateRepository plateRepository;
    private IssueService service;

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        feeRepository = mock(BookingFeeRepository.class);
        plateRepository = mock(LicensePlateRepository.class);
        service = new IssueServiceImpl(issueRepository, mock(BookingRepository.class), mock(EmployeeRepository.class),
                feeRepository, mock(IssueEvidenceRepository.class), mock(FileUploadService.class), plateRepository,
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void thePageIsNewestFirstAndEachRowIsMapped() {
        IssueQueryDTO query = new IssueQueryDTO();
        query.setPage(2);
        query.setSize(20);
        Issue issue = issue(7L, "2026-10-20T14:30:00+07:00");
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(issue), PageRequest.of(1, 20), 21));

        Page<IssueDTO> page = service.searchForAdmin(query);

        assertEquals(List.of(7L), page.getContent().stream().map(IssueDTO::getId).toList());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(issueRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")), pageable.getValue().getSort());
    }

    @Test
    void aPagePastTheEndStepsBackToTheLastOne() {
        IssueQueryDTO query = new IssueQueryDTO();
        query.setPage(9);
        Issue issue = issue(7L, "2026-10-20T14:30:00+07:00");
        when(issueRepository.findAll(any(Specification.class), eq(PageRequest.of(8, 10,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(8, 10), 25));
        when(issueRepository.findAll(any(Specification.class), eq(PageRequest.of(2, 10,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))))
                .thenReturn(new PageImpl<>(List.of(issue), PageRequest.of(2, 10), 25));

        Page<IssueDTO> page = service.searchForAdmin(query);

        assertEquals(2, page.getNumber());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void theRowShowsThePlateTheCarHadWhenTheIssueHappened() {
        Issue issue = issue(7L, "2026-10-20T14:30:00+07:00");
        LicensePlate thenPlate = new LicensePlate();
        thenPlate.setPlate("51A-OLD.01");
        when(plateRepository.findCarriedAt(9L, issue.getOccurredAt())).thenReturn(Optional.of(thenPlate));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(issue), PageRequest.of(0, 10), 1));

        IssueDTO row = service.searchForAdmin(new IssueQueryDTO()).getContent().get(0);

        assertEquals("51A-OLD.01", row.getLicensePlate());
    }

    @Test
    void theStatsCountOpenIssuesWhatIsStillOwedAndWhatWasCollectedThisMonth() {
        when(issueRepository.countByStatus(IssueStatus.OPEN)).thenReturn(4L);
        when(feeRepository.countIssueFeesAwaitingPayment(IssueStatus.RESOLVED)).thenReturn(2L);
        when(feeRepository.sumIssueFeesPaidBetween(any(), any())).thenReturn(new BigDecimal("1250000"));

        IssueStatsDTO stats = service.stats();

        assertEquals(4L, stats.open());
        assertEquals(2L, stats.awaitingPayment());
        assertEquals(new BigDecimal("1250000"), stats.collectedThisMonth());
    }

    @Test
    void thisMonthRunsFromTheFirstToTheFirstOfNextMonthInVietnamTime() {
        when(feeRepository.sumIssueFeesPaidBetween(any(), any())).thenReturn(BigDecimal.ZERO);

        service.stats();

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(feeRepository).sumIssueFeesPaidBetween(from.capture(), to.capture());
        ZonedDateTime start = from.getValue().atZoneSameInstant(DateTimeUtils.VIETNAM_ZONE);
        assertEquals(1, start.getDayOfMonth());
        assertEquals(0, start.getHour());
        assertEquals(start.plusMonths(1).toInstant(), to.getValue().toInstant());
        assertTrue(!start.toInstant().isAfter(OffsetDateTime.now().toInstant().plus(1, ChronoUnit.SECONDS)));
    }

    private Issue issue(Long id, String occurredAt) {
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Nguyễn Văn An");
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        Employee reporter = new Employee();
        reporter.setId(2L);
        reporter.setName("Trần Thị Bình");
        Issue issue = new Issue();
        issue.setId(id);
        issue.setBooking(booking);
        issue.setVehicle(vehicle);
        issue.setEmployee(reporter);
        issue.setIssueType(IssueType.TRAFFIC_FINE);
        issue.setSubject("Fine " + id);
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setOccurredAt(OffsetDateTime.parse(occurredAt));
        issue.setReportedAt(OffsetDateTime.parse(occurredAt));
        return issue;
    }
}
