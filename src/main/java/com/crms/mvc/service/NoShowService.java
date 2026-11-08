package com.crms.mvc.service;

import java.time.OffsetDateTime;

/**
 * Khách không đến nhận xe: a paid booking whose car was not handed over within the Thời gian chờ nhận xe of its
 * contract is cancelled without refunding the deposit, so the car goes back on the schedule.
 */
public interface NoShowService {

    /** Cancels every paid booking whose pickup wait has run out at {@code now}; returns how many. */
    int cancelNoShows(OffsetDateTime now);
}
