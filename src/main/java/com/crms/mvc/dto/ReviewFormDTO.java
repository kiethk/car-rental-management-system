package com.crms.mvc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewFormDTO {
    @NotNull(message = "Vui lòng chọn số sao đánh giá.")
    @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao.")
    @Max(value = 5, message = "Đánh giá cao nhất là 5 sao.")
    private Integer rating;

    @Size(max = 1000, message = "Nội dung đánh giá không được vượt quá 1000 ký tự.")
    private String comment;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
