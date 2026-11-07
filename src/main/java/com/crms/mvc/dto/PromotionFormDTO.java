package com.crms.mvc.dto;

import com.crms.mvc.entity.Promotion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PromotionFormDTO {
    private Long id;

    @NotBlank(message = "Promotion code cannot be blank")
    @Size(max = 50, message = "Promotion code cannot exceed 50 characters")
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Use only letters, numbers, hyphens, and underscores")
    private String code;

    @NotBlank(message = "Please select a discount type")
    @Pattern(regexp = "percent|fixed", message = "Discount type must be percent or fixed")
    private String discountType;

    @NotNull(message = "Discount value cannot be empty")
    @DecimalMin(value = "0.00", message = "Discount value cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Discount value supports up to 12 integer digits and 2 decimals")
    private BigDecimal discountValue;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "Quantity cannot be empty")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    private String urlImagePromotion;
    private MultipartFile imageFile;

    public static PromotionFormDTO fromEntity(Promotion promotion) {
        PromotionFormDTO dto = new PromotionFormDTO();
        dto.setId(promotion.getId());
        dto.setCode(promotion.getCode());
        dto.setDiscountType(promotion.getDiscountType());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setQuantity(promotion.getQuantity());
        dto.setUrlImagePromotion(promotion.getUrlImagePromotion());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getUrlImagePromotion() { return urlImagePromotion; }
    public void setUrlImagePromotion(String urlImagePromotion) { this.urlImagePromotion = urlImagePromotion; }
    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
}
