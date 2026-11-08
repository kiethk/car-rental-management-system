package com.crms.mvc.event;

import com.crms.mvc.dto.TrafficFineNoticeDTO;

/** A traffic fine was recorded: the customer is told which rental it belongs to and gets the police notice photo. */
public record TrafficFineRecordedEvent(TrafficFineNoticeDTO notice, String imageFileName) {
}
