package com.crms.mvc.repository;

import com.crms.mvc.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Promotion> findAllByOrderByIdDesc();

    /** Locks the row so two bookings cannot both use the last remaining use of a code. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Promotion p WHERE LOWER(p.code) = LOWER(:code)")
    Optional<Promotion> findByCodeForUpdate(@org.springframework.data.repository.query.Param("code") String code);
}
