package com.example.scoring.exception;

import java.util.List;

/**
 * Raised when a scoring request fails business-logic (cross-field
 * plausibility) validation, as opposed to simple format validation which is
 * handled by Bean Validation annotations on the request DTO.
 */
public class ScoringValidationException extends RuntimeException {

    private final List<String> violations;

    public ScoringValidationException(List<String> violations) {
        super("Business validation failed: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
