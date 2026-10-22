package com.crms.mvc.dto;

public record LoginIdentityDTO(
        String accountType,
        Long accountId,
        String name,
        String role
) {
}
