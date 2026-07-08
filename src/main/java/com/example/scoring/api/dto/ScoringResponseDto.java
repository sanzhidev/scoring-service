package com.example.scoring.api.dto;

import com.example.scoring.model.Decision;

import java.util.List;

public record ScoringResponseDto(
        String requestId,
        int score,
        Decision decision,
        String modelVersion,
        List<ScoringFactorDto> factors
) {
}
