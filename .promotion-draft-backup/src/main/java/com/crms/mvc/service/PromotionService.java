package com.crms.mvc.service;

import com.crms.mvc.dto.PromotionFormDTO;
import com.crms.mvc.entity.Promotion;

import java.util.List;
import java.util.Optional;

public interface PromotionService {
    List<Promotion> findAll();

    List<Promotion> findActiveWithImages();

    Optional<Promotion> findById(Long id);

    Promotion create(PromotionFormDTO form);

    Promotion update(Long id, PromotionFormDTO form);

    void delete(Long id);
}
