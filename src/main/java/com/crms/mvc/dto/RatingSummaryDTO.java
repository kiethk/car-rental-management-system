package com.crms.mvc.dto;

import java.io.Serializable;

/** Average star rating and number of reviews of one vehicle type, as shown on its cards. */
public class RatingSummaryDTO implements Serializable {
    public static final RatingSummaryDTO NONE = new RatingSummaryDTO(0, 0);

    private final double average;
    private final long count;

    public RatingSummaryDTO(double average, long count) {
        this.average = average;
        this.count = count;
    }

    public double getAverage() { return average; }
    public long getCount() { return count; }
}
