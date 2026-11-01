package com.crms.mvc.dto;

import java.io.Serializable;

public class CheckOutDTO implements Serializable {
    private String returnNotes;

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }
}

