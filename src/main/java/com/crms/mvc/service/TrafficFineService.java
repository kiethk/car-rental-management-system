package com.crms.mvc.service;

import com.crms.mvc.dto.TrafficFineFormDTO;
import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.entity.Booking;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/** Traffic fines (phạt nguội): matched to a rental, recorded as a notice to the customer, never charged. */
public interface TrafficFineService {

    /**
     * Finds who had the car when a traffic fine was issued: the vehicle that carried the plate at that moment
     * and the rental that held it then.
     *
     * @throws NoSuchElementException when no car carried the plate, or no rental held it, at that time
     */
    Booking findBookingForViolation(String plate, LocalDateTime violationAt);

    /** Records the fine with its police notice photo and emails the customer. */
    TrafficFineNoticeDTO record(TrafficFineFormDTO form, Long employeeId);

    /** Deletes a record made by mistake and emails the customer. */
    void delete(Long noticeId);

    List<TrafficFineNoticeDTO> findAll();

    /** The fines on one rental, shown on the customer's booking page. Empty when the booking is not theirs. */
    List<TrafficFineNoticeDTO> findByBookingForCustomer(Long bookingId, Long customerId);

    /** The photo of the police notice. */
    Resource loadImage(Long noticeId);
}
