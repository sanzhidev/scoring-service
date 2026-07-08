package com.example.scoring.exception;

import com.example.scoring.api.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleFormatValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(), "Request format validation failed", details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ScoringValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessValidation(ScoringValidationException ex) {
        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(), "Business validation failed", ex.getViolations());
        return ResponseEntity.badRequest().body(body);
    }
}
