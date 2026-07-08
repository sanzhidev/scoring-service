package com.example.scoring.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedScoringModelV1Test {

    private final RuleBasedScoringModelV1 model = new RuleBasedScoringModelV1();

    @Test
    void reportsItsVersion() {
        assertThat(model.getVersion()).isEqualTo("rule-based-v1");
        assertThat(RuleBasedScoringModelV1.VERSION).isEqualTo("rule-based-v1");
    }

    @Test
    void strongApplicantIsApprovedWithMaxScore() {
        ScoringFeatures features = new ScoringFeatures(
                "req-approve-1",
                30,
                BigDecimal.valueOf(300_000),
                BigDecimal.valueOf(500_000),
                36,
                false,
                0
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.decision()).isEqualTo(Decision.APPROVED);
        assertThat(result.modelVersion()).isEqualTo("rule-based-v1");
        assertThat(result.factors()).extracting(ScoringFactor::code).containsExactlyInAnyOrder(
                "AGE_IN_OPTIMAL_RANGE",
                "LOW_AMOUNT_TO_INCOME_RATIO",
                "LONG_EMPLOYMENT_HISTORY",
                "NO_OVERDUE_PAYMENTS",
                "NO_ACTIVE_LOANS"
        );
    }

    @Test
    void borderlineApplicantGoesToManualReview() {
        ScoringFeatures features = new ScoringFeatures(
                "req-manual-1",
                40,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(600_000),
                4,
                false,
                3
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isEqualTo(40);
        assertThat(result.decision()).isEqualTo(Decision.MANUAL_REVIEW);
    }

    @Test
    void weakApplicantIsRejectedWithMinScore() {
        ScoringFeatures features = new ScoringFeatures(
                "req-reject-1",
                19,
                BigDecimal.valueOf(80_000),
                BigDecimal.valueOf(900_000),
                2,
                true,
                6
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.decision()).isEqualTo(Decision.REJECTED);
        assertThat(result.factors()).extracting(ScoringFactor::code).contains(
                "HAS_OVERDUE_PAYMENTS", "MANY_ACTIVE_LOANS", "VERY_HIGH_AMOUNT_TO_INCOME_RATIO");
    }

    @Test
    void scoreExactlyAtApproveThresholdIsApproved() {
        // age neutral(0) + dti moderate(+5) + employment neutral(0) + no overdue(+5) + no loans(+10) = 70
        ScoringFeatures features = new ScoringFeatures(
                "req-boundary-approve",
                22,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(500_000),
                12,
                false,
                0
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isEqualTo(70);
        assertThat(result.decision()).isEqualTo(Decision.APPROVED);
    }

    @Test
    void scoreExactlyAtManualReviewThresholdIsManualReview() {
        // age optimal(+10) + dti high(-10) + employment neutral(0) + no overdue(+5) + several loans(-15) = 40
        ScoringFeatures features = new ScoringFeatures(
                "req-boundary-manual",
                30,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(800_000),
                12,
                false,
                3
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isEqualTo(40);
        assertThat(result.decision()).isEqualTo(Decision.MANUAL_REVIEW);
    }

    @Test
    void scoreBelowManualReviewThresholdIsRejected() {
        // same as above but very-high dti and many active loans pushes the score under 40
        ScoringFeatures features = new ScoringFeatures(
                "req-boundary-reject",
                30,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(1_500_000),
                12,
                false,
                6
        );

        ScoringResult result = model.score(features);

        assertThat(result.score()).isLessThan(40);
        assertThat(result.decision()).isEqualTo(Decision.REJECTED);
    }
}
