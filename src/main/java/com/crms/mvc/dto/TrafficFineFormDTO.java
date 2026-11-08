package com.crms.mvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/** What staff enter from the police notice. The rental's own times and contract are read from the rental. */
public class TrafficFineFormDTO {
    @NotNull(message = "Chưa chọn đơn thuê")
    private Long bookingId;

    @NotBlank(message = "Vui lòng nhập biển số trên biên bản")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String plate;

    /** Vietnam time: the form's datetime-local input has no zone. */
    @NotNull(message = "Vui lòng nhập thời điểm vi phạm")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime violationAt;

    private MultipartFile image;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public LocalDateTime getViolationAt() { return violationAt; }
    public void setViolationAt(LocalDateTime violationAt) { this.violationAt = violationAt; }

    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
