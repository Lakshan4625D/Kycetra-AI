package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.KycQuestionRequest;
import com.lakshan.kycetra_ai.dto.KycQuestionResponse;
import com.lakshan.kycetra_ai.service.KycQuestionAnswerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class KycQuestionController {

    private final KycQuestionAnswerService questionAnswerService;

    public KycQuestionController(
            KycQuestionAnswerService questionAnswerService) {
        this.questionAnswerService = questionAnswerService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @Valid @RequestBody KycQuestionRequest request,
            Authentication authentication) {

        try {
            KycQuestionResponse response =
                    questionAnswerService.answer(
                            authentication.getName(),
                            request.question());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));
        }
    }
}