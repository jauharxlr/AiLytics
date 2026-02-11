package com.ailytics.ailytics.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.Media;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

@Service
public class GeminiService {

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String processDocument(Resource fileResource, String contentType, String prompt) {
        return this.chatClient.prompt()
                .user(u -> u.text(prompt)
                        .media(new Media(MimeTypeUtils.parseMimeType(contentType), fileResource)))
                .call()
                .content();
    }

    /**
     * Processes document and returns a Map based on the provided JSON structure hint.
     * Future-proofed for strict schema enforcement.
     */
    public Map<String, Object> processDocumentToStructuredJson(Resource fileResource, String contentType, String prompt) {
        return this.chatClient.prompt()
                .user(u -> u.text(prompt + " \nReturn the result in JSON format.")
                        .media(new Media(MimeTypeUtils.parseMimeType(contentType), fileResource)))
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
