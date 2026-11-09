package com.crms.mvc.util;

import com.crms.mvc.entity.VehicleType;

/** The picture of a vehicle type, or a stock photo while none has been uploaded. */
public final class VehicleImages {
    public static final String DEFAULT_URL =
            "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";

    private VehicleImages() {
    }

    public static String urlOf(VehicleType type) {
        return type == null || type.getImageUrl() == null || type.getImageUrl().isBlank() ? DEFAULT_URL : type.getImageUrl();
    }
}
