package com.crms.mvc.service.impl;

import com.crms.mvc.enums.DiscountType;
import com.crms.mvc.dto.PromotionFormDTO;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final FileUploadService fileUploadService;

    public PromotionServiceImpl(PromotionRepository promotionRepository,
                                BookingRepository bookingRepository,
                                FileUploadService fileUploadService) {
        this.promotionRepository = promotionRepository;
        this.bookingRepository = bookingRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return promotionRepository.findAllByOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findActiveWithImages() {
        LocalDate today = LocalDate.now();
        return promotionRepository.findAllByOrderByIdDesc().stream()
                .filter(promotion -> promotion.getQuantity() != null && promotion.getQuantity() > 0)
                .filter(promotion -> promotion.getStartDate() != null
                        && !promotion.getStartDate().isAfter(today))
                .filter(promotion -> promotion.getEndDate() != null
                        && !promotion.getEndDate().isBefore(today))
                .filter(promotion -> promotion.getUrlImagePromotion() != null
                        && !promotion.getUrlImagePromotion().isBlank())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    @Override
    public Promotion create(PromotionFormDTO form) {
        validate(form, null);
        Promotion promotion = toEntity(form, new Promotion());
        saveUploadedImage(form.getImageFile(), promotion);
        return promotionRepository.save(promotion);
    }

    @Override
    public Promotion update(Long id, PromotionFormDTO form) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi có mã: " + id));
        validate(form, id);
        saveUploadedImage(form.getImageFile(), promotion);
        return promotionRepository.save(toEntity(form, promotion));
    }

    @Override
    public void delete(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi có mã: " + id));
        if (bookingRepository.existsByPromotionId(id)) {
            throw new IllegalStateException("Không xóa được khuyến mãi này vì đã được dùng trong đơn đặt xe.");
        }
        promotionRepository.delete(promotion);
    }

    private void validate(PromotionFormDTO form, Long excludedId) {
        String code = form.getCode().trim();
        boolean duplicate = excludedId == null
                ? promotionRepository.existsByCodeIgnoreCase(code)
                : promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, excludedId);
        if (duplicate) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại: " + code);
        }
        if (form.getStartDate().isAfter(form.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc cùng ngày kết thúc.");
        }
        if ("percent".equalsIgnoreCase(form.getDiscountType())
                && form.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Giảm giá theo phần trăm không được vượt quá 100%.");
        }
    }

    private Promotion toEntity(PromotionFormDTO form, Promotion promotion) {
        promotion.setCode(form.getCode().trim().toUpperCase());
        promotion.setDiscountType(DiscountType.from(form.getDiscountType()));
        promotion.setDiscountValue(form.getDiscountValue());
        promotion.setStartDate(form.getStartDate());
        promotion.setEndDate(form.getEndDate());
        promotion.setQuantity(form.getQuantity());
        return promotion;
    }

    private void saveUploadedImage(MultipartFile image, Promotion promotion) {
        if (image != null && !image.isEmpty()) {
            promotion.setUrlImagePromotion(fileUploadService.savePromotionImage(image));
        }
    }
}
