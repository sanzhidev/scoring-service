package com.example.scoring.model;

/**
 * Versioned scoring model contract. Implementations are pure functions of
 * {@link ScoringFeatures} to {@link ScoringResult} with no knowledge of the
 * API/transport layer, so a new model version (or a future ML/ONNX-backed
 * implementation) can be swapped in without touching the rest of the service.
 */
public interface ScoringModel {

    /**
     * @return a stable identifier of this model implementation and version,
     * e.g. {@code "rule-based-v1"}. Returned to callers so decisions remain
     * traceable to the exact model that produced them.
     */
    String getVersion();

    /**
     * Computes a score and decision for the given features.
     *
     * @param features validated input features
     * @return the scoring result, including the factors that drove it
     */
    ScoringResult score(ScoringFeatures features);
}
