package com.lakshan.kycetra_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KycQuestionRequest(
        @NotBlank(message = "Question must not be blank.")
        @Size(max = 2000, message = "Question must not exceed 2000 characters.")
        String question
) {
}