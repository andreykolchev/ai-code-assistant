package org.aicodeassistant;

import io.qdrant.client.grpc.Points;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CodeAssistant {

    @ConfigProperty(name = "ollama.chat-model.model-id")
    String chatModel;

    @ConfigProperty(name = "ollama.embedding-model.model-id")
    String embeddingModel;

    @Inject
    QdrantService qdrantService;

    @Inject
    @RestClient
    OllamaClient ollamaClient;

    public String processQuestion(String question) {
        try {
            // 1. Generate embedding for the question
            var embeddingResponse = ollamaClient.getEmbedding(new OllamaClient.EmbeddingRequest(embeddingModel, question));
            if (embeddingResponse == null || embeddingResponse.embedding == null) {
                return "Error: Ollama returned an empty response for your question embedding.";
            }
            List<Float> questionVector = embeddingResponse.embedding;

            // 2. Search for relevant code in Qdrant
            List<Points.ScoredPoint> results = qdrantService.getClient().searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(qdrantService.getCollectionName())
                            .addAllVector(questionVector)
                            .setLimit(3)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .build()
            ).get();

            String context = results.stream()
                    .map(result -> {
                        var payload = result.getPayloadMap();
                        if (payload.containsKey("content")) return payload.get("content").getStringValue();
                        if (payload.containsKey("text_segment")) return payload.get("text_segment").getStringValue();
                        return "";
                    })
                    .collect(Collectors.joining("\n\n"));

            // 3. Construct prompt and call LLM
            String systemPrompt = "You are a code assistant. Your knowledge base contains methods from a Java project.\n" +
                    "Answer concisely and provide code examples where appropriate.\n" +
                    "Use the following code context to answer the question:\n\n" + context;

            List<OllamaClient.Message> messages = new ArrayList<>();
            messages.add(new OllamaClient.Message("system", systemPrompt));
            messages.add(new OllamaClient.Message("user", "Question about the project: " + question));

            var chatResponse = ollamaClient.chat(new OllamaClient.ChatRequest(chatModel, messages));
            if (chatResponse == null || chatResponse.message == null) {
                return "Error: Ollama returned an empty chat response.";
            }
            return chatResponse.message.content;
        } catch (Exception e) {
            return "Error processing question: " + e.getMessage();
        }
    }
}
