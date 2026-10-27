package com.crms.mvc.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeUtils() {}

    public static OffsetDateTime toVietnamTime(OffsetDateTime time) {
        return time != null ? time.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime() : null;
    }
}
