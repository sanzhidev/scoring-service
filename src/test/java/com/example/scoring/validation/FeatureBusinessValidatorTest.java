package com.example.scoring.validation;

import com.example.scoring.exception.ScoringValidationException;
import com.example.scoring.model.ScoringFeatures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureBusinessValidatorTest {

    private final FeatureBusinessValidator validator = new FeatureBusinessValidator();

    @Test
    void plausibleFeaturesPassValidation() {
        ScoringFeatures features = new ScoringFeatures(
                "req-1", 30, BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000), 24, false, 1);

        assertThatCode(() -> validator.validate(features)).doesNotThrowAnyException();
    }

    @Test
    void employmentDurationImplausibleForAgeIsRejected() {
        // clientAge 20 => max plausible employment is (20-16)*12 = 48 months
        ScoringFeatures features = new ScoringFeatures(
                "req-2", 20, BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000), 60, false, 1);

        assertThatThrownBy(() -> validator.validate(features))
                .isInstanceOf(ScoringValidationException.class)
                .satisfies(ex -> {
                    ScoringValidationException validationException = (ScoringValidationException) ex;
                    assertThat(validationException.getViolations())
                            .anyMatch(v -> v.contains("employmentMonths"));
                });
    }

    @Test
    void requestedAmountImplausibleRelativeToIncomeIsRejected() {
        ScoringFeatures features = new ScoringFeatures(
                "req-3", 30, BigDecimal.valueOf(1_000), BigDecimal.valueOf(5_000_000), 24, false, 1);

        assertThatThrownBy(() -> validator.validate(features))
                .isInstanceOf(ScoringValidationException.class)
                .satisfies(ex -> {
                    ScoringValidationException validationException = (ScoringValidationException) ex;
                    assertThat(validationException.getViolations())
                            .anyMatch(v -> v.contains("requestedAmount"));
                });
    }

    @Test
    void collectsAllViolationsInOneException() {
        ScoringFeatures features = new ScoringFeatures(
                "req-4", 20, BigDecimal.valueOf(1_000), BigDecimal.valueOf(5_000_000), 500, false, 1);

        assertThatThrownBy(() -> validator.validate(features))
                .isInstanceOf(ScoringValidationException.class)
                .satisfies(ex -> {
                    ScoringValidationException validationException = (ScoringValidationException) ex;
                    assertThat(validationException.getViolations()).hasSize(2);
                });
    }
}
