package com.example.scoring.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Wire format for a scoring request. Field-level annotations cover format
 * validation only (types, ranges, presence); cross-field business rules are
 * checked separately by {@code FeatureBusinessValidator} after mapping to
 * the model's own {@code ScoringFeatures} type.
 */
public record ScoringRequestDto(

        @NotBlank(message = "requestId must not be blank")
        String requestId,

        @NotNull(message = "clientAge is required")
        @Min(value = 18, message = "clientAge must be at least 18")
        @Max(value = 100, message = "clientAge must be at most 100")
        Integer clientAge,

        @NotNull(message = "monthlyIncome is required")
        @DecimalMin(value = "0.01", message = "monthlyIncome must be positive")
        BigDecimal monthlyIncome,

        @NotNull(message = "requestedAmount is required")
        @DecimalMin(value = "0.01", message = "requestedAmount must be positive")
        BigDecimal requestedAmount,

        @NotNull(message = "employmentMonths is required")
        @Min(value = 0, message = "employmentMonths must not be negative")
        Integer employmentMonths,

        @NotNull(message = "hasOverduePayments is required")
        Boolean hasOverduePayments,

        @NotNull(message = "activeLoansCount is required")
        @Min(value = 0, message = "activeLoansCount must not be negative")
        @Max(value = 50, message = "activeLoansCount must be at most 50")
        Integer activeLoansCount
) {
}
