package com.crms.mvc.dto;

import com.crms.mvc.enums.IssueStatus;
import com.crms.mvc.enums.IssueType;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * What the admin issue list asks for, bound from the query string so every filtered page can be bookmarked.
 * A value that is not recognised means "no filter" rather than an error. The list is always newest first.
 */
public class IssueQueryDTO extends PageQueryDTO {
    public static final String ALL = "all";

    private String status = ALL;
    private String type = ALL;
    private String plate;
    private Long bookingId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public String getStatus() { return status; }

    public void setStatus(String status) {
        String normalized = status == null ? ALL : status.trim().toLowerCase();
        boolean known = Arrays.stream(IssueStatus.values()).anyMatch(s -> s.name().toLowerCase().equals(normalized));
        this.status = known ? normalized : ALL;
    }

    /** The status to filter by, or null for all. */
    public IssueStatus getIssueStatus() {
        return ALL.equals(status) ? null : IssueStatus.valueOf(status.toUpperCase());
    }

    public String getType() { return type; }

    public void setType(String type) {
        String normalized = type == null ? ALL : type.trim().toUpperCase();
        boolean known = Arrays.stream(IssueType.values()).anyMatch(t -> t.name().equals(normalized));
        this.type = known ? normalized : ALL;
    }

    /** The type to filter by, or null for all. */
    public IssueType getIssueType() {
        return ALL.equals(type) ? null : IssueType.valueOf(type);
    }

    /** Trimmed plate text, or null when the box is empty. */
    public String getPlate() {
        return plate == null || plate.isBlank() ? null : plate.trim();
    }

    public void setPlate(String plate) { this.plate = plate; }

    /** The plate as a lower-case "contains" pattern for LIKE ... ESCAPE '\'; null when there is no plate filter. */
    public String getPlatePattern() {
        String text = getPlate();
        if (text == null) {
            return null;
        }
        String escaped = text.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }

    /** Start of the first day, Vietnam time; null when there is no start date. */
    public OffsetDateTime getFromInstant() {
        return from == null ? null : from.atStartOfDay(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
    }

    /** Start of the day after the last day, so the last day itself is included; null when there is no end date. */
    public OffsetDateTime getToInstant() {
        return to == null ? null : to.plusDays(1).atStartOfDay(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
    }

    @Override
    protected void addFilterParams(UriComponentsBuilder builder) {
        super.addFilterParams(builder);
        builder.queryParam("status", status);
        builder.queryParam("type", type);
        if (getPlate() != null) {
            builder.queryParam("plate", getPlate());
        }
        if (bookingId != null) {
            builder.queryParam("bookingId", bookingId);
        }
        if (from != null) {
            builder.queryParam("from", from);
        }
        if (to != null) {
            builder.queryParam("to", to);
        }
    }
}
