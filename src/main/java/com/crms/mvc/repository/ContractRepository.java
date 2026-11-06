package com.crms.mvc.repository;

import com.crms.mvc.entity.Contract;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @EntityGraph(attributePaths = {"template", "booking.customer"})
    Optional<Contract> findByBookingId(Long bookingId);

    /**
     * Locks the contract row while it is issued, so a staff "issue" click and the background issue after the deposit
     * run one after the other instead of both generating a PDF and emailing the customer twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.booking.id = :bookingId")
    Optional<Contract> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);
}
