package com.marchepro.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "La question est obligatoire")
        @Size(max = 1000)
        @JsonAlias("message")
        String question
) {
}
