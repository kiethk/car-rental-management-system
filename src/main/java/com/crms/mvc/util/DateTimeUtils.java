package com.crms.mvc.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** The fixed offset of {@link #VIETNAM_ZONE} (Vietnam has no daylight saving), for building OffsetDateTime values. */
    public static final ZoneOffset VIETNAM_OFFSET = ZoneOffset.ofHours(7);
    public static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private DateTimeUtils() {}

    public static OffsetDateTime toVietnamTime(OffsetDateTime time) {
        return time != null ? time.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime() : null;
    }

    public static String formatToVietnamTime(OffsetDateTime time) {
        return time != null ? toVietnamTime(time).format(DISPLAY_FORMAT) : "";
    }
}
