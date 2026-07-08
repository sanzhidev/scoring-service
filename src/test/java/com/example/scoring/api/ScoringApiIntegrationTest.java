package com.example.scoring.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests exercising the real Spring context (controller, service,
 * validators and the rule-based model together) with no mocking, so a
 * wiring mistake between layers would surface here even if the isolated
 * unit tests still pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScoringApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void approvedScenarioEndToEnd() throws Exception {
        String json = """
                {
                  "requestId": "req-approve-1",
                  "clientAge": 30,
                  "monthlyIncome": 300000,
                  "requestedAmount": 500000,
                  "employmentMonths": 36,
                  "hasOverduePayments": false,
                  "activeLoansCount": 0
                }
                """;

        mockMvc.perform(post("/api/v1/scoring").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.modelVersion").value("rule-based-v1"));
    }

    @Test
    void manualReviewScenarioEndToEnd() throws Exception {
        String json = """
                {
                  "requestId": "req-manual-1",
                  "clientAge": 40,
                  "monthlyIncome": 100000,
                  "requestedAmount": 600000,
                  "employmentMonths": 4,
                  "hasOverduePayments": false,
                  "activeLoansCount": 3
                }
                """;

        mockMvc.perform(post("/api/v1/scoring").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(40))
                .andExpect(jsonPath("$.decision").value("MANUAL_REVIEW"));
    }

    @Test
    void rejectedScenarioEndToEnd() throws Exception {
        String json = """
                {
                  "requestId": "req-reject-1",
                  "clientAge": 19,
                  "monthlyIncome": 80000,
                  "requestedAmount": 900000,
                  "employmentMonths": 2,
                  "hasOverduePayments": true,
                  "activeLoansCount": 6
                }
                """;

        mockMvc.perform(post("/api/v1/scoring").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.decision").value("REJECTED"));
    }

    @Test
    void businessValidationFailureEndToEnd() throws Exception {
        String json = """
                {
                  "requestId": "req-business-invalid",
                  "clientAge": 20,
                  "monthlyIncome": 1000,
                  "requestedAmount": 5000000,
                  "employmentMonths": 500,
                  "hasOverduePayments": false,
                  "activeLoansCount": 1
                }
                """;

        mockMvc.perform(post("/api/v1/scoring").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business validation failed"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }
}
