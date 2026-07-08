package com.example.scoring.api;

import com.example.scoring.api.dto.ScoringRequestDto;
import com.example.scoring.api.dto.ScoringResponseDto;
import com.example.scoring.service.ScoringService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping
    public ResponseEntity<ScoringResponseDto> score(@Valid @RequestBody ScoringRequestDto request) {
        return ResponseEntity.ok(scoringService.score(request));
    }
}
