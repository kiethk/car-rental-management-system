package com.crms.mvc.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private DateTimeUtils() {}

    public static OffsetDateTime toVietnamTime(OffsetDateTime time) {
        return time != null ? time.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime() : null;
    }

    public static String formatToVietnamTime(OffsetDateTime time) {
        return time != null ? toVietnamTime(time).format(DISPLAY_FORMAT) : "";
    }
}
