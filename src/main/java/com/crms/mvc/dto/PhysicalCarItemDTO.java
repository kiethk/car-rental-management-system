package com.crms.mvc.dto;

public record PhysicalCarItemDTO(
        Long id,
        String licensePlate,
        String color,
        Integer year,
        String status
) {}
