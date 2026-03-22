package org.aicodeassistant;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class GeminiClient {

    @ConfigProperty(name = "gemini.chat.model")
    String model;
    @ConfigProperty(name = "gemini.api-key")
    String apiKey;

    private Models models;

    @PostConstruct
    void init() {
        models = Client.builder().apiKey(apiKey).build().models;
    }

    public String chat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be null or blank");
        }
        GenerateContentResponse geminiResp = models.generateContent(model, prompt, GenerateContentConfig.builder().build());
        return Optional.ofNullable(geminiResp.text()).orElseThrow(() -> new IllegalStateException("Empty response from Gemini."));
    }
}
