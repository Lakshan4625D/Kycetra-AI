package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.AiSmokeTestResponse;
import com.lakshan.kycetra_ai.service.GeminiSmokeTestService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
public class AiSmokeTestController {

    private final GeminiSmokeTestService smokeTestService;

    public AiSmokeTestController(
            GeminiSmokeTestService smokeTestService) {
        this.smokeTestService = smokeTestService;
    }

    @PostMapping("/smoke-test")
    public ResponseEntity<AiSmokeTestResponse> smokeTest() {
        try {
            String response = smokeTestService.testConnection();

            if (response == null || response.isBlank()) {
                throw new IllegalStateException(
                    "AI provider returned an empty response."
                );
            }

            return ResponseEntity.ok(
                new AiSmokeTestResponse("SUCCESS", response.trim())
            );

        } catch (Exception exception) {
              exception.printStackTrace();

            // Log the exception server-side if needed, but never
            // return the provider exception or API key to the client.
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI provider request failed."
            );
        }
    }
}