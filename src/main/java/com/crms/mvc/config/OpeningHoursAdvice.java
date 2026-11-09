package com.crms.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the depot info and opening hours available to every page as model attributes. */
@ControllerAdvice
public class OpeningHoursAdvice {

    private final OpeningHours openingHours;
    private final StationProperties station;

    public OpeningHoursAdvice(@Value("${crms.opening-hours.open:0}") int open,
                              @Value("${crms.opening-hours.close:23}") int close,
                              StationProperties station) {
        this.openingHours = new OpeningHours(open, close);
        this.station = station;
    }

    @ModelAttribute("openingHours")
    public OpeningHours openingHours() {
        return openingHours;
    }

    @ModelAttribute("stationName")
    public String stationName() {
        return station.name();
    }

    @ModelAttribute("stationAddress")
    public String stationAddress() {
        return station.address();
    }

    @ModelAttribute("stationPhone")
    public String stationPhone() {
        return station.phone();
    }

    @ModelAttribute("stationMapEmbedUrl")
    public String stationMapEmbedUrl() {
        return station.mapEmbedUrl();
    }
}
