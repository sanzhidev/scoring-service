package com.example.scoring.api;

import com.example.scoring.api.dto.ScoringFactorDto;
import com.example.scoring.api.dto.ScoringRequestDto;
import com.example.scoring.api.dto.ScoringResponseDto;
import com.example.scoring.model.ScoringFactor;
import com.example.scoring.model.ScoringFeatures;
import com.example.scoring.model.ScoringResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates between the API wire format and the model's own domain types,
 * keeping the two layers decoupled.
 */
@Component
public class ScoringMapper {

    public ScoringFeatures toFeatures(ScoringRequestDto request) {
        return new ScoringFeatures(
                request.requestId(),
                request.clientAge(),
                request.monthlyIncome(),
                request.requestedAmount(),
                request.employmentMonths(),
                request.hasOverduePayments(),
                request.activeLoansCount()
        );
    }

    public ScoringResponseDto toResponse(String requestId, ScoringResult result) {
        List<ScoringFactorDto> factors = result.factors().stream()
                .map(this::toFactorDto)
                .toList();

        return new ScoringResponseDto(
                requestId,
                result.score(),
                result.decision(),
                result.modelVersion(),
                factors
        );
    }

    private ScoringFactorDto toFactorDto(ScoringFactor factor) {
        return new ScoringFactorDto(factor.code(), factor.description(), factor.impact());
    }
}
