package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CheckOutDTO implements Serializable {
    private String returnNotes;
    private BigDecimal additionalFine = BigDecimal.ZERO;

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }

    public BigDecimal getAdditionalFine() { return additionalFine; }
    public void setAdditionalFine(BigDecimal additionalFine) { this.additionalFine = additionalFine; }
}

