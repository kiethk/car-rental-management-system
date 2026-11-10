package com.crms.mvc.repository;

import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {
    long countByVehicleId(Long vehicleId);

    boolean existsByBookingIdAndIssueTypeInAndStatusNot(Long bookingId, Collection<IssueType> types, IssueStatus status);

    String WITH_DETAILS = "SELECT i FROM Issue i JOIN FETCH i.booking b JOIN FETCH b.customer "
            + "JOIN FETCH i.vehicle v JOIN FETCH v.vehicleType JOIN FETCH i.employee ";

    @Query(WITH_DETAILS + "WHERE i.id = :id")
    Optional<Issue> findByIdWithDetails(@Param("id") Long id);

    @Query(WITH_DETAILS + "WHERE b.id = :bookingId ORDER BY i.occurredAt DESC")
    List<Issue> findByBookingIdWithDetails(@Param("bookingId") Long bookingId);

    long countByStatus(IssueStatus status);

    /** The list page loads everything it shows in one query (the count query ignores the graph). */
    @Override
    @EntityGraph(attributePaths = {"booking", "booking.customer", "vehicle", "vehicle.vehicleType", "employee"})
    Page<Issue> findAll(Specification<Issue> spec, Pageable pageable);

}
