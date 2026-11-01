package com.crms.mvc.repository;
import com.crms.mvc.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    long countByVehicleId(Long vehicleId);
    Optional<Issue> findByBookingIdAndIssueType(Long bookingId, String issueType);
    List<Issue> findByBookingIdOrderByReportedAtAsc(Long bookingId);
}
