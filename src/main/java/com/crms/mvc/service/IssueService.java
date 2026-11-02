package com.crms.mvc.service;

import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.IssueEvidence;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Records and settles what a customer owes for an incident during a rental (see GLOSSARY.md: Sự cố).
 * An Issue tracks the customer's money liability only; its fee lives in booking_fees and this service is
 * the only writer of that fee.
 */
public interface IssueService {
    /** Records an Issue for a booking the customer has picked up (active or completed). */
    Issue create(IssueFormDTO form, IssueActor actor);

    /**
     * Finds who had the car when a traffic fine was issued: the vehicle that carried the plate at that moment
     * (plates have a history), then the rental covering it. Times are Vietnam time.
     *
     * @throws java.util.NoSuchElementException when the plate was on no car then, or the car was not rented
     */
    Booking findBookingForViolation(String plate, LocalDateTime occurredAt);

    /** Edits an open Issue, including its fee. */
    Issue update(Long issueId, IssueFormDTO form);

    /** Records direct payment at the counter (cash or bank transfer) for an open issue. */
    Issue settleDirectly(Long issueId, String paymentMethod, String note, IssueActor actor);

    /** Internal / settlement: open -> resolved when payment is confirmed or checkout completes. */
    Issue resolve(Long issueId, String resolutionNote, IssueActor actor);

    /**
     * Cancels an Issue (and removes its fee). Staff may cancel an open Issue; cancelling a resolved one is
     * admin-only and only while no payment has taken the fee.
     */
    Issue cancel(Long issueId, String reason, IssueActor actor);

    /**
     * Adds one photo (JPEG/PNG/WebP, up to 5 MB, at most 10 per issue). Allowed while the issue is open, or resolved
     * but not yet being paid; never on a cancelled issue.
     */
    IssueEvidence addEvidence(Long issueId, MultipartFile image);

    /** Removes a photo; only while the issue is open, because a resolved fee rests on its photos. */
    void removeEvidence(Long issueId, Long evidenceId);

    /** The photo file for staff. */
    Resource loadEvidence(Long issueId, Long evidenceId);

    /** The photo file for the customer who owns the booking; anyone else gets "not found". */
    Resource loadEvidenceForCustomer(Long issueId, Long evidenceId, Long customerId);

    Optional<IssueDTO> findDetail(Long issueId);

    /** Every Issue of a booking, newest first. */
    List<IssueDTO> findByBooking(Long bookingId);

    /**
     * What a customer may see of their own booking: its issues except cancelled ones, newest first.
     *
     * @throws java.util.NoSuchElementException when the booking is not theirs
     */
    List<IssueDTO> findByBookingForCustomer(Long bookingId, Long customerId);

    /**
     * True when the booking has a collision or damage issue that is not cancelled, so the car must be inspected
     * before it is rented again.
     */
    boolean bookingHasVehicleDamage(Long bookingId);

    /**
     * Emails customers about open Issues whose notice grace period has passed and who have not been told yet.
     * Runs on a schedule; an Issue cancelled inside the grace period is never announced.
     */
    void sendDueNotices();

    /** The data a customer email about this Issue needs, read inside a transaction. */
    Optional<IssueNoticeDTO> findNotice(Long issueId);

    /** One page of the admin list: filtered, newest first. A page past the end steps back to the last one. */
    Page<IssueDTO> searchForAdmin(IssueQueryDTO query);

    /** Open issues, resolved issues still waiting for the customer's money, and what was collected this month. */
    /** How many Issues are still open (fee not settled yet). */
    long countOpen();
}
