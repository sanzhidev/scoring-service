package com.example.scoring.model;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * First rule-based scoring model version. This is a hand-written heuristic,
 * NOT a statistically trained or validated model - see README for the full
 * list of limitations before considering it for anything beyond a demo.
 */
@Component
public class RuleBasedScoringModelV1 implements ScoringModel {

    public static final String VERSION = "rule-based-v1";

    private static final int BASE_SCORE = 50;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private static final int APPROVE_THRESHOLD = 70;
    private static final int MANUAL_REVIEW_THRESHOLD = 40;

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public ScoringResult score(ScoringFeatures features) {
        List<ScoringFactor> factors = new ArrayList<>();

        int rawScore = BASE_SCORE
                + scoreAge(features.clientAge(), factors)
                + scoreDebtToIncome(features.monthlyIncome(), features.requestedAmount(), factors)
                + scoreEmployment(features.employmentMonths(), factors)
                + scoreOverduePayments(features.hasOverduePayments(), factors)
                + scoreActiveLoans(features.activeLoansCount(), factors);

        int finalScore = clamp(rawScore);
        Decision decision = decide(finalScore);

        return new ScoringResult(finalScore, decision, VERSION, List.copyOf(factors));
    }

    private int scoreAge(int age, List<ScoringFactor> factors) {
        if (age < 21 || age > 65) {
            return addFactor(factors, "AGE_OUTSIDE_OPTIMAL_RANGE",
                    "Client age " + age + " is outside the 21-65 range considered lower risk", -10);
        }
        if (age >= 25 && age <= 55) {
            return addFactor(factors, "AGE_IN_OPTIMAL_RANGE",
                    "Client age " + age + " is within the 25-55 range considered lower risk", 10);
        }
        return addFactor(factors, "AGE_NEUTRAL",
                "Client age " + age + " has a neutral effect on the score", 0);
    }

    private int scoreDebtToIncome(BigDecimal monthlyIncome, BigDecimal requestedAmount, List<ScoringFactor> factors) {
        BigDecimal ratio = requestedAmount.divide(monthlyIncome, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(BigDecimal.valueOf(3)) <= 0) {
            return addFactor(factors, "LOW_AMOUNT_TO_INCOME_RATIO",
                    "Requested amount to monthly income ratio " + ratio + " is <= 3", 20);
        }
        if (ratio.compareTo(BigDecimal.valueOf(6)) <= 0) {
            return addFactor(factors, "MODERATE_AMOUNT_TO_INCOME_RATIO",
                    "Requested amount to monthly income ratio " + ratio + " is <= 6", 5);
        }
        if (ratio.compareTo(BigDecimal.valueOf(10)) <= 0) {
            return addFactor(factors, "HIGH_AMOUNT_TO_INCOME_RATIO",
                    "Requested amount to monthly income ratio " + ratio + " is <= 10", -10);
        }
        return addFactor(factors, "VERY_HIGH_AMOUNT_TO_INCOME_RATIO",
                "Requested amount to monthly income ratio " + ratio + " exceeds 10", -30);
    }

    private int scoreEmployment(int employmentMonths, List<ScoringFactor> factors) {
        if (employmentMonths < 6) {
            return addFactor(factors, "SHORT_EMPLOYMENT_HISTORY",
                    "Employment duration of " + employmentMonths + " month(s) is under 6 months", -15);
        }
        if (employmentMonths <= 24) {
            return addFactor(factors, "MODERATE_EMPLOYMENT_HISTORY",
                    "Employment duration of " + employmentMonths + " month(s) is between 6 and 24 months", 0);
        }
        return addFactor(factors, "LONG_EMPLOYMENT_HISTORY",
                "Employment duration of " + employmentMonths + " month(s) exceeds 24 months", 15);
    }

    private int scoreOverduePayments(boolean hasOverduePayments, List<ScoringFactor> factors) {
        if (hasOverduePayments) {
            return addFactor(factors, "HAS_OVERDUE_PAYMENTS",
                    "Client has a history of overdue payments", -35);
        }
        return addFactor(factors, "NO_OVERDUE_PAYMENTS",
                "Client has no history of overdue payments", 5);
    }

    private int scoreActiveLoans(int activeLoansCount, List<ScoringFactor> factors) {
        if (activeLoansCount == 0) {
            return addFactor(factors, "NO_ACTIVE_LOANS",
                    "Client has no active loans", 10);
        }
        if (activeLoansCount <= 2) {
            return addFactor(factors, "FEW_ACTIVE_LOANS",
                    "Client has " + activeLoansCount + " active loan(s)", 0);
        }
        if (activeLoansCount <= 4) {
            return addFactor(factors, "SEVERAL_ACTIVE_LOANS",
                    "Client has " + activeLoansCount + " active loans", -15);
        }
        return addFactor(factors, "MANY_ACTIVE_LOANS",
                "Client has " + activeLoansCount + " active loans (5 or more)", -30);
    }

    private int addFactor(List<ScoringFactor> factors, String code, String description, int impact) {
        factors.add(new ScoringFactor(code, description, impact));
        return impact;
    }

    private Decision decide(int score) {
        if (score >= APPROVE_THRESHOLD) {
            return Decision.APPROVED;
        }
        if (score >= MANUAL_REVIEW_THRESHOLD) {
            return Decision.MANUAL_REVIEW;
        }
        return Decision.REJECTED;
    }

    private int clamp(int score) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }
}
