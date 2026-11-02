package com.marchepro.chatbot.controller;

import com.marchepro.chatbot.dto.ChatRequest;
import com.marchepro.chatbot.dto.ChatResponse;
import com.marchepro.chatbot.service.ChatbotService;
import com.marchepro.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Assistant MarchéPro", description = "Assistant conversationnel connecté aux données réelles")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    @Operation(summary = "Poser une question à l'assistant")
    public ApiResponse<ChatResponse> demander(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatbotService.repondre(request.question()));
    }
}
