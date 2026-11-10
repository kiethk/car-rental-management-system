package com.crms.mvc.repository;

import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.entity.LicensePlate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;

/** Turns the filters of the admin issue list into a query; a filter that is not set adds no condition. */
public final class IssueSpecifications {
    private IssueSpecifications() {}

    public static Specification<Issue> matching(IssueQueryDTO query) {
        return (root, criteria, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (query.getIssueStatus() != null) {
                conditions.add(cb.equal(root.get("status"), query.getIssueStatus()));
            }
            if (query.getIssueType() != null) {
                conditions.add(cb.equal(root.get("issueType"), query.getIssueType()));
            }
            if (query.getBookingId() != null) {
                conditions.add(cb.equal(root.get("booking").get("id"), query.getBookingId()));
            }
            if (query.getFromInstant() != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), query.getFromInstant()));
            }
            if (query.getToInstant() != null) {
                conditions.add(cb.lessThan(root.get("occurredAt"), query.getToInstant()));
            }
            if (query.getPlatePattern() != null) {
                // The plate the car carried when it happened, not the one it has today
                Subquery<Long> carried = criteria.subquery(Long.class);
                Root<LicensePlate> plate = carried.from(LicensePlate.class);
                carried.select(plate.get("id")).where(
                        cb.equal(plate.get("vehicle"), root.get("booking").get("vehicle")),
                        cb.like(cb.lower(plate.get("plate")), query.getPlatePattern(), '\\'),
                        cb.lessThanOrEqualTo(plate.get("validFrom"), root.<OffsetDateTime>get("occurredAt")),
                        cb.or(cb.isNull(plate.get("validTo")),
                                cb.greaterThan(plate.<OffsetDateTime>get("validTo"), root.<OffsetDateTime>get("occurredAt"))));
                conditions.add(cb.exists(carried));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }
}
