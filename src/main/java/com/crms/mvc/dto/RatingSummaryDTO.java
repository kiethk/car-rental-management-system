package com.crms.mvc.dto;

import java.io.Serializable;

/** Average star rating and number of reviews of one vehicle type, as shown on its cards. */
public record RatingSummaryDTO(double average, long count) implements Serializable {
    public static final RatingSummaryDTO NONE = new RatingSummaryDTO(0, 0);
}
