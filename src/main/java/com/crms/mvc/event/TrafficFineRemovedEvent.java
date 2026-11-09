package com.crms.mvc.event;

import com.crms.mvc.dto.TrafficFineNoticeDTO;

/** A traffic fine record was deleted (recorded by mistake): the customer is told to ignore the earlier email. */
public record TrafficFineRemovedEvent(TrafficFineNoticeDTO notice) {
}
