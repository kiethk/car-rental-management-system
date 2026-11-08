package com.crms.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the depot info and opening hours available to every page as model attributes. */
@ControllerAdvice
public class OpeningHoursAdvice {

    private final OpeningHours openingHours;
    private final String stationName;
    private final String stationAddress;
    private final String stationPhone;
    private final String stationMapEmbedUrl;

    public OpeningHoursAdvice(@Value("${crms.opening-hours.open:6}") int open,
                              @Value("${crms.opening-hours.close:22}") int close,
                              @Value("${crms.station.name:Kho xe DriveNow - ĐH FPT TP.HCM}") String stationName,
                              @Value("${crms.station.address:Lô E2a-7, Đường D1, Khu Công nghệ cao, Long Thạnh Mỹ, TP. Thủ Đức, TP.HCM}") String stationAddress,
                              @Value("${crms.station.phone:1900 6868}") String stationPhone,
                              @Value("${crms.station.map-embed-url:https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3724.7204527239955!2d106.80730807480559!3d10.841127589311382!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x31752731176b07b1%3A0xb752b24b379bae5e!2sFPT%20University%20HCMC!5e1!3m2!1sen!2s!4v1794108631900!5m2!1sen!2s}") String stationMapEmbedUrl) {
        this.openingHours = new OpeningHours(open, close);
        this.stationName = stationName;
        this.stationAddress = stationAddress;
        this.stationPhone = stationPhone;
        this.stationMapEmbedUrl = stationMapEmbedUrl;
    }

    @ModelAttribute("openingHours")
    public OpeningHours openingHours() {
        return openingHours;
    }

    @ModelAttribute("stationName")
    public String stationName() {
        return stationName;
    }

    @ModelAttribute("stationAddress")
    public String stationAddress() {
        return stationAddress;
    }

    @ModelAttribute("stationPhone")
    public String stationPhone() {
        return stationPhone;
    }

    @ModelAttribute("stationMapEmbedUrl")
    public String stationMapEmbedUrl() {
        return stationMapEmbedUrl;
    }
}
