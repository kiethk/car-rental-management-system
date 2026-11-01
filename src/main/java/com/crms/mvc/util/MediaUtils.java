package com.crms.mvc.util;

import org.springframework.http.MediaType;

import java.util.Locale;

public final class MediaUtils {
    private MediaUtils() {}

    public static MediaType resolveMediaType(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
