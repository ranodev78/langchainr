package dev.rano.langchainr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainRequest(
        @NotBlank(message = "Question cannot be empty")
        @Size(min = 1, max = 2000, message = "Question must be 2000 characters or less")
        String question
) {
}
