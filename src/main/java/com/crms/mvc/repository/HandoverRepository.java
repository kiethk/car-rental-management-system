package com.crms.mvc.repository;

import com.crms.mvc.entity.Handover;
import com.crms.mvc.entity.HandoverType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HandoverRepository extends JpaRepository<Handover, Long> {
    Optional<Handover> findByBookingIdAndType(Long bookingId, HandoverType type);

    boolean existsByBookingIdAndType(Long bookingId, HandoverType type);

    @EntityGraph(attributePaths = "employee")
    List<Handover> findByBookingIdOrderByRecordedAtAsc(Long bookingId);

    Optional<Handover> findByIdAndBookingId(Long id, Long bookingId);
}
