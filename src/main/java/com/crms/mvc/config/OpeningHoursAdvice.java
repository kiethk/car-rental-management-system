package com.crms.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the depot's opening hours available to every page as ${openingHours}, read from one place. */
@ControllerAdvice
public class OpeningHoursAdvice {

    private final OpeningHours openingHours;

    public OpeningHoursAdvice(@Value("${crms.opening-hours.open:0}") int open,
                              @Value("${crms.opening-hours.close:23}") int close) {
        this.openingHours = new OpeningHours(open, close);
    }

    @ModelAttribute("openingHours")
    public OpeningHours openingHours() {
        return openingHours;
    }
}
