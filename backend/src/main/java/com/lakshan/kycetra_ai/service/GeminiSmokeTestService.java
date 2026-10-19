package com.lakshan.kycetra_ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiSmokeTestService {

    private final ChatClient chatClient;

    public GeminiSmokeTestService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String testConnection() {
        return chatClient.prompt()
            .user("Reply with exactly: KYCETRA_GEMINI_OK")
            .call()
            .content();
    }
}