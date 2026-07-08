package com.example.scoring.model;

import java.math.BigDecimal;

/**
 * Feature vector consumed by a {@link ScoringModel}. This is the model's own
 * input contract, deliberately kept separate from any API DTO so the model
 * package has zero dependency on the web layer.
 */
public record ScoringFeatures(
        String requestId,
        int clientAge,
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        int employmentMonths,
        boolean hasOverduePayments,
        int activeLoansCount
) {
}
