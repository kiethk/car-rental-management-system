package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.event.CompensationVoucherIssuedEvent;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.repository.PromotionRepository;
import com.crms.mvc.service.CompensationVoucherService;
import com.crms.mvc.service.ContractService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Transactional
public class CompensationVoucherServiceImpl implements CompensationVoucherService {
    /** Unambiguous characters only: no 0/O or 1/I, since the customer may type the code. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PromotionRepository promotionRepository;
    private final ContractService contractService;
    private final ApplicationEventPublisher eventPublisher;

    public CompensationVoucherServiceImpl(PromotionRepository promotionRepository, ContractService contractService,
                                          ApplicationEventPublisher eventPublisher) {
        this.promotionRepository = promotionRepository;
        this.contractService = contractService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Promotion issueFor(Booking booking) {
        // The promise is the one in the contract the customer accepted, not the current version.
        ContractTemplateDTO terms = contractService.getTermsOfBooking(booking.getId());
        int percent = terms.getCompensationVoucherPercent();
        LocalDate today = LocalDate.now(VIETNAM);
        Promotion voucher = new Promotion();
        voucher.setCode(newCode(booking.getId()));
        voucher.setDiscountType("percent");
        voucher.setDiscountValue(BigDecimal.valueOf(percent));
        voucher.setStartDate(today);
        voucher.setEndDate(today.plusDays(terms.getCompensationVoucherDays() - 1L));
        voucher.setQuantity(1);
        Promotion saved = promotionRepository.save(voucher);

        eventPublisher.publishEvent(new CompensationVoucherIssuedEvent(booking.getId(),
                booking.getCustomer() != null ? booking.getCustomer().getName() : null,
                booking.getCustomer() != null ? booking.getCustomer().getEmail() : null,
                saved.getCode(), percent, saved.getEndDate()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findForBooking(Long bookingId) {
        return promotionRepository.findFirstByCodeStartingWithIgnoreCase(prefix(bookingId));
    }

    /** "BU42-K7QM2X": the booking id ties the voucher to its cancelled booking, the random part keeps it unguessable. */
    private String newCode(Long bookingId) {
        String code;
        do {
            StringBuilder random = new StringBuilder();
            for (int i = 0; i < 6; i++) random.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            code = prefix(bookingId) + random;
        } while (promotionRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    private static String prefix(Long bookingId) {
        return "BU" + bookingId + "-";
    }
}
