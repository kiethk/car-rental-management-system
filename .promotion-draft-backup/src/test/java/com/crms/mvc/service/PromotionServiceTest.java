package com.crms.mvc.service;

import com.crms.mvc.dto.PromotionFormDTO;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromotionServiceTest {
    private PromotionRepository promotionRepository;
    private BookingRepository bookingRepository;
    private FileUploadService fileUploadService;
    private PromotionService service;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        bookingRepository = mock(BookingRepository.class);
        fileUploadService = mock(FileUploadService.class);
        service = new PromotionServiceImpl(promotionRepository, bookingRepository, fileUploadService);
    }

    @Test
    void createNormalizesCodeAndDiscountType() {
        PromotionFormDTO form = form();
        form.setCode(" welcome10 ");
        form.setDiscountType("PERCENT");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(call -> call.getArgument(0));

        Promotion result = service.create(form);

        assertEquals("WELCOME10", result.getCode());
        assertEquals("percent", result.getDiscountType());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void createStoresUploadedImageUrlOnPromotion() {
        PromotionFormDTO form = form();
        form.setImageFile(new MockMultipartFile("imageFile", "promo.png", "image/png", new byte[]{1}));
        when(fileUploadService.savePromotionImage(form.getImageFile())).thenReturn("/uploads/promotions/promo.png");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(call -> call.getArgument(0));

        Promotion result = service.create(form);

        assertEquals("/uploads/promotions/promo.png", result.getUrlImagePromotion());
        verify(fileUploadService).savePromotionImage(form.getImageFile());
    }

    @Test
    void findActiveWithImagesExcludesExpiredEmptyAndUnavailablePromotions() {
        LocalDate today = LocalDate.now();
        Promotion activeWithImage = promotion(today.minusDays(1), today.plusDays(1), 2, "/uploads/promotions/active.png");
        Promotion expired = promotion(today.minusDays(2), today.minusDays(1), 2, "/uploads/promotions/expired.png");
        Promotion unavailable = promotion(today, today.plusDays(1), 0, "/uploads/promotions/unavailable.png");
        Promotion withoutImage = promotion(today, today.plusDays(1), 2, " ");
        when(promotionRepository.findAllByOrderByIdDesc())
                .thenReturn(List.of(activeWithImage, expired, unavailable, withoutImage));

        assertEquals(List.of(activeWithImage), service.findActiveWithImages());
    }

    @Test
    void createRejectsDuplicateCodeIgnoringCase() {
        when(promotionRepository.existsByCodeIgnoreCase("WELCOME10")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(form()));
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void createRejectsInvalidDateRangeAndPercentage() {
        PromotionFormDTO invalidDates = form();
        invalidDates.setStartDate(LocalDate.of(2026, 12, 2));
        invalidDates.setEndDate(LocalDate.of(2026, 12, 1));
        assertThrows(IllegalArgumentException.class, () -> service.create(invalidDates));

        PromotionFormDTO invalidDiscount = form();
        invalidDiscount.setDiscountValue(new BigDecimal("100.01"));
        assertThrows(IllegalArgumentException.class, () -> service.create(invalidDiscount));
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void deleteRejectsPromotionAlreadyUsedByBooking() {
        Promotion promotion = new Promotion();
        promotion.setId(7L);
        when(promotionRepository.findById(7L)).thenReturn(java.util.Optional.of(promotion));
        when(bookingRepository.existsByPromotionId(7L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.delete(7L));
        verify(promotionRepository, never()).delete(any(Promotion.class));
    }

    private PromotionFormDTO form() {
        PromotionFormDTO form = new PromotionFormDTO();
        form.setCode("WELCOME10");
        form.setDiscountType("percent");
        form.setDiscountValue(new BigDecimal("10"));
        form.setStartDate(LocalDate.of(2026, 12, 1));
        form.setEndDate(LocalDate.of(2026, 12, 31));
        form.setQuantity(10);
        return form;
    }

    private Promotion promotion(LocalDate startDate, LocalDate endDate, int quantity, String imageUrl) {
        Promotion promotion = new Promotion();
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setQuantity(quantity);
        promotion.setUrlImagePromotion(imageUrl);
        return promotion;
    }
}
