package com.crms.mvc.repository;

import com.crms.mvc.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCase(String code);
}
