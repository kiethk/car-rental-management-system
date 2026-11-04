package com.crms.mvc.dto;

import org.springframework.web.util.UriComponentsBuilder;

public class AccountQueryDTO extends PageQueryDTO {
    private String type = SessionAccount.CUSTOMER;

    public String getType() { return type; }

    public void setType(String type) {
        this.type = SessionAccount.EMPLOYEE.equals(type) ? SessionAccount.EMPLOYEE : SessionAccount.CUSTOMER;
    }

    @Override
    protected void addFilterParams(UriComponentsBuilder builder) {
        super.addFilterParams(builder);
        builder.queryParam("type", type);
    }
}
