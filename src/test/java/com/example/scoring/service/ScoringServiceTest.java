package com.example.scoring.service;

import com.example.scoring.api.ScoringMapper;
import com.example.scoring.api.dto.ScoringRequestDto;
import com.example.scoring.api.dto.ScoringResponseDto;
import com.example.scoring.exception.ScoringValidationException;
import com.example.scoring.model.Decision;
import com.example.scoring.model.ScoringFactor;
import com.example.scoring.model.ScoringFeatures;
import com.example.scoring.model.ScoringModel;
import com.example.scoring.model.ScoringResult;
import com.example.scoring.validation.FeatureBusinessValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private ScoringModel scoringModel;

    @Mock
    private FeatureBusinessValidator businessValidator;

    private final ScoringMapper mapper = new ScoringMapper();

    private ScoringService scoringService(ScoringModel model, FeatureBusinessValidator validator) {
        return new ScoringService(model, validator, mapper);
    }

    private ScoringRequestDto sampleRequest() {
        return new ScoringRequestDto(
                "req-1", 30, BigDecimal.valueOf(200_000), BigDecimal.valueOf(400_000), 24, false, 1);
    }

    @Test
    void validatesBeforeScoringAndReturnsMappedResponse() {
        ScoringRequestDto request = sampleRequest();
        ScoringResult modelResult = new ScoringResult(
                85, Decision.APPROVED, "rule-based-v1",
                List.of(new ScoringFactor("NO_OVERDUE_PAYMENTS", "no overdue", 5)));
        when(scoringModel.score(any(ScoringFeatures.class))).thenReturn(modelResult);

        ScoringService service = scoringService(scoringModel, businessValidator);
        ScoringResponseDto response = service.score(request);

        assertThat(response.requestId()).isEqualTo("req-1");
        assertThat(response.score()).isEqualTo(85);
        assertThat(response.decision()).isEqualTo(Decision.APPROVED);
        assertThat(response.modelVersion()).isEqualTo("rule-based-v1");
        assertThat(response.factors()).hasSize(1);

        InOrder inOrder = inOrder(businessValidator, scoringModel);
        inOrder.verify(businessValidator).validate(any(ScoringFeatures.class));
        inOrder.verify(scoringModel).score(any(ScoringFeatures.class));
    }

    @Test
    void doesNotCallModelWhenBusinessValidationFails() {
        ScoringRequestDto request = sampleRequest();
        org.mockito.Mockito.doThrow(new ScoringValidationException(List.of("bad request")))
                .when(businessValidator).validate(any(ScoringFeatures.class));

        ScoringService service = scoringService(scoringModel, businessValidator);

        assertThatThrownBy(() -> service.score(request))
                .isInstanceOf(ScoringValidationException.class);

        verifyNoInteractions(scoringModel);
    }
}
