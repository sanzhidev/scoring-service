package com.example.scoring.service;

import com.example.scoring.api.ScoringMapper;
import com.example.scoring.api.dto.ScoringRequestDto;
import com.example.scoring.api.dto.ScoringResponseDto;
import com.example.scoring.model.ScoringFeatures;
import com.example.scoring.model.ScoringModel;
import com.example.scoring.model.ScoringResult;
import com.example.scoring.validation.FeatureBusinessValidator;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a scoring request: maps the API request to the model's
 * feature vector, runs business-logic validation, delegates to the
 * configured {@link ScoringModel}, and maps the result back to the API
 * response shape. Contains no scoring rules itself.
 */
@Service
public class ScoringService {

    private final ScoringModel scoringModel;
    private final FeatureBusinessValidator businessValidator;
    private final ScoringMapper mapper;

    public ScoringService(ScoringModel scoringModel,
                           FeatureBusinessValidator businessValidator,
                           ScoringMapper mapper) {
        this.scoringModel = scoringModel;
        this.businessValidator = businessValidator;
        this.mapper = mapper;
    }

    public ScoringResponseDto score(ScoringRequestDto request) {
        ScoringFeatures features = mapper.toFeatures(request);
        businessValidator.validate(features);
        ScoringResult result = scoringModel.score(features);
        return mapper.toResponse(request.requestId(), result);
    }
}
