package com.crms.mvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The depot where cars are handed over and taken back (the FPT University campus): the one place its name, address,
 * phone and map are configured, under {@code crms.station.*}. Pages, emails and the contract PDF all read it from here.
 */
@ConfigurationProperties("crms.station")
public record StationProperties(
        @DefaultValue(DEFAULT_NAME) String name,
        @DefaultValue(DEFAULT_ADDRESS) String address,
        @DefaultValue(DEFAULT_PHONE) String phone,
        @DefaultValue(DEFAULT_MAP_EMBED_URL) String mapEmbedUrl) {

    public static final String DEFAULT_NAME = "Kho xe DriveNow - ĐH FPT TP.HCM";
    public static final String DEFAULT_ADDRESS = "Lô E2a-7, Đường D1, Khu Công nghệ cao, Long Thạnh Mỹ, TP. Thủ Đức, TP.HCM";
    public static final String DEFAULT_PHONE = "1900 6868";
    public static final String DEFAULT_MAP_EMBED_URL = "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3724.7204527239955"
            + "!2d106.80730807480559!3d10.841127589311382!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2"
            + "!1s0x31752731176b07b1%3A0xb752b24b379bae5e!2sFPT%20University%20HCMC!5e1!3m2!1sen!2s!4v1794108631900!5m2!1sen!2s";

    /** What the properties bind to when nothing is configured; also what unit tests use. */
    public static final StationProperties DEFAULT =
            new StationProperties(DEFAULT_NAME, DEFAULT_ADDRESS, DEFAULT_PHONE, DEFAULT_MAP_EMBED_URL);
}
