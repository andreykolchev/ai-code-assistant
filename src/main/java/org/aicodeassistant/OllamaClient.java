package org.aicodeassistant;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;
import java.util.Map;

@RegisterRestClient(configKey = "ollama-api")
public interface OllamaClient {

    @POST
    @Path("/api/embeddings")
    EmbeddingResponse getEmbedding(EmbeddingRequest request);

    @POST
    @Path("/api/chat")
    ChatResponse chat(ChatRequest request);

    class EmbeddingRequest {
        public String model;
        public String prompt;

        public EmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }
    }

    class EmbeddingResponse {
        public List<Float> embedding;
    }

    class ChatRequest {
        public String model;
        public List<Message> messages;
        public boolean stream = false;
        public Map<String, Object> options;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    class ChatResponse {
        public Message message;
    }
}
