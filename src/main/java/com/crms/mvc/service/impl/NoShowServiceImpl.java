package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.event.NoShowCancelledEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.NoShowService;
import com.crms.mvc.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class NoShowServiceImpl implements NoShowService {
    private static final Logger log = LoggerFactory.getLogger(NoShowServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final ContractService contractService;
    private final ApplicationEventPublisher eventPublisher;

    public NoShowServiceImpl(BookingRepository bookingRepository, ContractService contractService,
                             ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.contractService = contractService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60_000)
    public void cancelNoShowsNow() {
        cancelNoShows(OffsetDateTime.now());
    }

    @Override
    public int cancelNoShows(OffsetDateTime now) {
        int cancelled = 0;
        // Paid bookings whose pickup time has passed but which were never handed over.
        for (Booking booking : bookingRepository.findConfirmedWithPickupBefore(now)) {
            ContractTemplateDTO terms = contractService.getTermsOfBooking(booking.getId());
            OffsetDateTime deadline = terms.pickupDeadline(booking.getPickupTime(), booking.getScheduledEndTime());
            if (now.isBefore(deadline)) {
                continue;
            }
            booking.setStatus("cancelled");
            booking.setCancelledAt(now);
            bookingRepository.save(booking);
            cancelled++;
            log.info("Booking #{} cancelled: customer did not pick up the car by {}", booking.getId(), deadline);
            eventPublisher.publishEvent(new NoShowCancelledEvent(booking.getId(),
                    booking.getCustomer() != null ? booking.getCustomer().getName() : null,
                    booking.getCustomer() != null ? booking.getCustomer().getEmail() : null,
                    booking.getSlot().getVehicleType().getDisplayName(),
                    DateTimeUtils.toVietnamTime(booking.getPickupTime()), DateTimeUtils.toVietnamTime(now),
                    booking.getDepositAmount(), terms.getPickupWaitHours()));
        }
        return cancelled;
    }
}
