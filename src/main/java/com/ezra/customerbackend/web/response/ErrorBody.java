package com.ezra.customerbackend.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorBody(
        String code,
        Instant timestamp,
        String path,
        List<FieldViolation> fieldViolations
) {

    public static ErrorBody of(String code, Instant timestamp, String path, List<FieldViolation> violations) {
        return new ErrorBody(code, timestamp, path,
                violations == null || violations.isEmpty() ? null : List.copyOf(violations));
    }

    public record FieldViolation(String field, String message) {
    }
}
