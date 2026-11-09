package com.crms.mvc.service;

import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.dto.ReviewFormDTO;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Review;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.repository.ReviewRepository;
import com.crms.mvc.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {
    private BookingRepository bookingRepository;
    private PaymentRepository paymentRepository;
    private ReviewRepository reviewRepository;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        service = new ReviewServiceImpl(bookingRepository, paymentRepository, reviewRepository);
    }

    @Test
    void submitsOneReviewForOwnedCompletedBooking() {
        Booking booking = booking(BookingStatus.COMPLETED, 7L, 3L);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingId(7L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(call -> call.getArgument(0));
        ReviewFormDTO form = form(5, "  Great service.  ");

        service.submit(7L, 3L, form);

        org.mockito.ArgumentCaptor<Review> captor = org.mockito.ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(booking, captor.getValue().getBooking());
        assertEquals(5, captor.getValue().getRating());
        assertEquals("Great service.", captor.getValue().getComment());
        verify(reviewRepository).findByBookingId(7L);
    }

    @Test
    void rejectsReviewBeforeBookingIsCompleted() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking(BookingStatus.ACTIVE, 7L, 3L)));

        assertThrows(IllegalStateException.class, () -> service.submit(7L, 3L, form(5, null)));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsReviewWhileAnyPaymentIsPending() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking(BookingStatus.COMPLETED, 7L, 3L)));
        when(paymentRepository.existsByBookingIdAndStatus(7L, PaymentStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.submit(7L, 3L, form(5, null)));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsReviewWhenBookingBelongsToAnotherCustomer() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking(BookingStatus.COMPLETED, 7L, 4L)));

        assertThrows(IllegalArgumentException.class, () -> service.submit(7L, 3L, form(5, null)));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateReview() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking(BookingStatus.COMPLETED, 7L, 3L)));
        when(reviewRepository.findByBookingId(7L)).thenReturn(Optional.of(new Review()));

        assertThrows(IllegalStateException.class, () -> service.submit(7L, 3L, form(5, null)));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void existingReviewCanOnlyBeShownForItsOwnerAfterCompletion() {
        Booking booking = booking(BookingStatus.COMPLETED, 7L, 3L);
        Review review = new Review();
        review.setRating(4);
        review.setComment("Good");
        review.setCreatedAt(OffsetDateTime.now());
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(7L, PaymentStatus.PENDING)).thenReturn(false);
        when(reviewRepository.findByBookingId(7L)).thenReturn(Optional.of(review));

        assertEquals(4, service.findForCustomer(7L, 3L).getRating());
        assertThrows(IllegalArgumentException.class, () -> service.findForCustomer(7L, 4L));
        when(bookingRepository.findById(8L)).thenReturn(Optional.of(booking(BookingStatus.ACTIVE, 8L, 3L)));
        assertNull(service.findForCustomer(8L, 3L));
    }

    @Test
    void publicVehicleTypeReviewsIncludeAverageAndCount() {
        when(reviewRepository.findPublicReviewsByVehicleTypeId(2L)).thenReturn(
                java.util.List.of(new ReviewDTO(5, "Great", null), new ReviewDTO(4, "Good", null)));

        VehicleReviewSummaryDTO summary = service.findPublicReviewsForVehicleType(2L);

        assertEquals(new java.math.BigDecimal("4.5"), summary.averageRating());
        assertEquals(2, summary.reviewCount());
        assertEquals(2, summary.reviews().size());
        verify(reviewRepository).findPublicReviewsByVehicleTypeId(2L);
    }

    @Test
    void publicVehicleTypeReviewSummaryHandlesNoReviews() {
        when(reviewRepository.findPublicReviewsByVehicleTypeId(2L)).thenReturn(java.util.List.of());

        VehicleReviewSummaryDTO summary = service.findPublicReviewsForVehicleType(2L);

        assertEquals(java.math.BigDecimal.ZERO, summary.averageRating());
        assertEquals(0, summary.reviewCount());
    }

    private Booking booking(BookingStatus status, Long bookingId, Long customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomer(customer);
        booking.setStatus(status);
        return booking;
    }

    private ReviewFormDTO form(int rating, String comment) {
        ReviewFormDTO form = new ReviewFormDTO();
        form.setRating(rating);
        form.setComment(comment);
        return form;
    }
}
