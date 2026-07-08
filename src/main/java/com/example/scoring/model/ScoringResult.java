package com.example.scoring.model;

import java.util.List;

public record ScoringResult(
        int score,
        Decision decision,
        String modelVersion,
        List<ScoringFactor> factors
) {
}
