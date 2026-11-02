package com.marchepro.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "La question est obligatoire")
        @Size(max = 1000)
        String question
) {
}
