package com.example.scoring.api.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        List<String> details
) {
    public static ErrorResponseDto of(int status, String error, List<String> details) {
        return new ErrorResponseDto(Instant.now(), status, error, details);
    }
}
