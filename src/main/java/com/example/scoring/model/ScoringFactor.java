package com.example.scoring.model;

/**
 * A single rule outcome that contributed to the final score.
 *
 * @param code        stable machine-readable identifier of the rule
 * @param description human-readable explanation of why the rule fired
 * @param impact      signed contribution to the raw score (before clamping)
 */
public record ScoringFactor(
        String code,
        String description,
        int impact
) {
}
