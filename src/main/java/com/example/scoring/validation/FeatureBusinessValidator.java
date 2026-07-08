package com.example.scoring.validation;

import com.example.scoring.exception.ScoringValidationException;
import com.example.scoring.model.ScoringFeatures;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-field plausibility checks that go beyond simple format validation
 * (each field in isolation looks fine, but the combination doesn't make
 * sense). Runs against the domain feature vector, after format validation
 * and before the request reaches the scoring model.
 */
@Component
public class FeatureBusinessValidator {

    private static final int MINIMUM_WORKING_AGE = 16;
    private static final BigDecimal MAX_AMOUNT_TO_INCOME_MULTIPLE = BigDecimal.valueOf(1000);

    public void validate(ScoringFeatures features) {
        List<String> violations = new ArrayList<>();

        validateEmploymentDurationAgainstAge(features, violations);
        validateRequestedAmountAgainstIncome(features, violations);

        if (!violations.isEmpty()) {
            throw new ScoringValidationException(violations);
        }
    }

    private void validateEmploymentDurationAgainstAge(ScoringFeatures features, List<String> violations) {
        int maxPlausibleEmploymentMonths = (features.clientAge() - MINIMUM_WORKING_AGE) * 12;
        if (features.employmentMonths() > maxPlausibleEmploymentMonths) {
            violations.add("employmentMonths (" + features.employmentMonths() + ") exceeds what is plausible "
                    + "for clientAge (" + features.clientAge() + "), assuming a minimum working age of "
                    + MINIMUM_WORKING_AGE);
        }
    }

    private void validateRequestedAmountAgainstIncome(ScoringFeatures features, List<String> violations) {
        BigDecimal maxPlausibleAmount = features.monthlyIncome().multiply(MAX_AMOUNT_TO_INCOME_MULTIPLE);
        if (features.requestedAmount().compareTo(maxPlausibleAmount) > 0) {
            violations.add("requestedAmount (" + features.requestedAmount() + ") is implausibly high relative to "
                    + "monthlyIncome (" + features.monthlyIncome() + ")");
        }
    }
}
