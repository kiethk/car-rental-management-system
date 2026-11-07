package com.crms.mvc.dto;

import java.time.OffsetDateTime;

public class ReviewDTO {
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;

    public ReviewDTO() {
    }

    public ReviewDTO(Integer rating, String comment, OffsetDateTime createdAt) {
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
