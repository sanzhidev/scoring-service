package com.example.scoring.api.dto;

public record ScoringFactorDto(
        String code,
        String description,
        int impact
) {
}
