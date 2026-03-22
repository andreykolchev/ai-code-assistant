package org.aicodeassistant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Points;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class CodeAssistant {

    @Inject
    @RestClient
    OllamaClient ollamaClient;

    @ConfigProperty(name = "ollama.chat-model.model-id")
    String chatModel;

    @ConfigProperty(name = "ollama.embedding-model.model-id")
    String embeddingModel;

    @ConfigProperty(name = "qdrant.host")
    String qdrantHost;

    @ConfigProperty(name = "qdrant.port")
    int qdrantPort;

    @ConfigProperty(name = "qdrant.collection.name")
    String collectionName;

    private QdrantClient qdrantClient;

    @PostConstruct
    void init() {
        qdrantClient = new QdrantClient(QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build());
    }

    public String processQuestion(String question) throws ExecutionException, InterruptedException {
        // 1. Generate embedding for the question
        var embeddingResponse = ollamaClient.getEmbedding(new OllamaClient.EmbeddingRequest(embeddingModel, question));
        List<Float> questionVector = embeddingResponse.embedding;

        // 2. Search for relevant code in Qdrant
        List<Points.ScoredPoint> results = qdrantClient.searchAsync(
            Points.SearchPoints.newBuilder()
                .setCollectionName(collectionName)
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
        return chatResponse.message.content;
    }
}
