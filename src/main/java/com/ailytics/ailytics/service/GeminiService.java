package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ExtractionResult;
import org.springframework.ai.chat.client.ChatClient;
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

    public ExtractionResult extractDataWithConfidence(Resource fileResource, String contentType, String schema) {
        String prompt = """
                Analyze the provided document and extract information according to this JSON schema: %s
                
                You must return a JSON object with the following structure:
                {
                  "data": { ... the extracted fields ... },
                  "confidence": 0.95
                }
                
                The 'confidence' field should be a decimal between 0.0 and 1.0 reflecting your certainty about the overall extraction accuracy.
                """.formatted(schema);
        
        return this.chatClient.prompt()
                .user(u -> u.text(prompt)
                        .media(new Media(MimeTypeUtils.parseMimeType(contentType), fileResource)))
                .call()
                .entity(ExtractionResult.class);
    }
}
