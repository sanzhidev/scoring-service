package com.example.scoring.api;

import com.example.scoring.api.dto.ScoringFactorDto;
import com.example.scoring.api.dto.ScoringResponseDto;
import com.example.scoring.exception.ScoringValidationException;
import com.example.scoring.model.Decision;
import com.example.scoring.service.ScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoringController.class)
class ScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScoringService scoringService;

    private static final String VALID_REQUEST_JSON = """
            {
              "requestId": "req-1",
              "clientAge": 30,
              "monthlyIncome": 300000,
              "requestedAmount": 500000,
              "employmentMonths": 36,
              "hasOverduePayments": false,
              "activeLoansCount": 0
            }
            """;

    @Test
    void returnsScoringResponseForValidRequest() throws Exception {
        ScoringResponseDto response = new ScoringResponseDto(
                "req-1", 100, Decision.APPROVED, "rule-based-v1",
                List.of(new ScoringFactorDto("NO_ACTIVE_LOANS", "no active loans", 10)));
        when(scoringService.score(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.modelVersion").value("rule-based-v1"))
                .andExpect(jsonPath("$.factors[0].code").value("NO_ACTIVE_LOANS"));
    }

    @Test
    void returnsBadRequestWhenFormatValidationFails() throws Exception {
        String invalidJson = """
                {
                  "requestId": "",
                  "clientAge": 10,
                  "monthlyIncome": -5,
                  "requestedAmount": 0,
                  "employmentMonths": -1,
                  "hasOverduePayments": false,
                  "activeLoansCount": 200
                }
                """;

        mockMvc.perform(post("/api/v1/scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request format validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsBadRequestWhenBusinessValidationFails() throws Exception {
        when(scoringService.score(any()))
                .thenThrow(new ScoringValidationException(List.of("requestedAmount is implausibly high")));

        mockMvc.perform(post("/api/v1/scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business validation failed"))
                .andExpect(jsonPath("$.details[0]").value("requestedAmount is implausibly high"));
    }
}
