package com.crms.mvc.dto;

import com.crms.mvc.util.EnumTokens;
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

    @NotBlank(message = "Vui lòng nhập mã khuyến mãi")
    @Size(max = 50, message = "Mã khuyến mãi tối đa 50 ký tự")
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Chỉ dùng chữ cái, chữ số, dấu gạch ngang và gạch dưới")
    private String code;

    @NotBlank(message = "Vui lòng chọn loại giảm giá")
    @Pattern(regexp = "percent|fixed", message = "Loại giảm giá phải là phần trăm hoặc số tiền cố định")
    private String discountType;

    @NotNull(message = "Vui lòng nhập giá trị giảm")
    @DecimalMin(value = "0.00", message = "Giá trị giảm không được âm")
    @Digits(integer = 12, fraction = 2, message = "Giá trị giảm tối đa 12 chữ số phần nguyên và 2 chữ số thập phân")
    private BigDecimal discountValue;

    @NotNull(message = "Vui lòng nhập ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Vui lòng nhập ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;

    private String urlImagePromotion;
    private MultipartFile imageFile;

    public static PromotionFormDTO fromEntity(Promotion promotion) {
        PromotionFormDTO dto = new PromotionFormDTO();
        dto.setId(promotion.getId());
        dto.setCode(promotion.getCode());
        dto.setDiscountType(EnumTokens.of(promotion.getDiscountType()));
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
