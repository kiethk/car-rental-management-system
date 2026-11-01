package com.crms.mvc.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IssueStatusConverter implements AttributeConverter<IssueStatus, String> {
    @Override
    public String convertToDatabaseColumn(IssueStatus status) {
        return status == null ? null : status.dbValue();
    }

    @Override
    public IssueStatus convertToEntityAttribute(String value) {
        return value == null ? null : IssueStatus.fromDbValue(value);
    }
}
