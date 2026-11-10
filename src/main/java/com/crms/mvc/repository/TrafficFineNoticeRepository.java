package com.crms.mvc.repository;

import com.crms.mvc.entity.TrafficFineNotice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrafficFineNoticeRepository extends JpaRepository<TrafficFineNotice, Long> {
    @EntityGraph(attributePaths = {"booking.customer", "booking.vehicle", "recordedBy"})
    List<TrafficFineNotice> findAllByOrderByRecordedAtDesc();

    @EntityGraph(attributePaths = {"booking.customer", "booking.vehicle", "recordedBy"})
    List<TrafficFineNotice> findByBookingIdOrderByViolationAtDesc(Long bookingId);

    @EntityGraph(attributePaths = {"booking.customer", "booking.vehicle", "recordedBy"})
    Optional<TrafficFineNotice> findWithDetailsById(Long id);
}
