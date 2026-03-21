package org.air;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@QuarkusTest
@TestProfile(MainAppIntegrationTest.RealServicesProfile.class)
class MainAppIntegrationTest {

    public static class RealServicesProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "ollama.chat-model.model-id", "qwen2.5-coder:3b",
                    "ollama.embedding-model.model-id", "nomic-embed-text",
                    "quarkus.rest-client.ollama-api.url", "http://localhost:11434",
                    "qdrant.host", "localhost",
                    "qdrant.port", "6334"
            );
        }
    }

    @Inject
    CodeAssistant assistant;

    @Inject
    CodeIndexer indexer;

    @Test
    void testIndexingAndAssistant(@TempDir Path tempDir) throws Exception {
        // 1. Create a sample Java file in the temporary directory
        Path javaFile = tempDir.resolve("Calculator.java");
        String content = """
                package org.air.test;
                
                public class Calculator {
                    /**
                     * Adds two integers.
                     */
                    public int add(int a, int b) {
                        return a + b;
                    }
                    
                    /**
                     * Subtracts two integers.
                     */
                    public int subtract(int a, int b) {
                        return a - b;
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // 2. Index the temporary directory
        System.out.println("Indexing test directory: " + tempDir.toAbsolutePath());
        indexer.indexDirectory(tempDir.toAbsolutePath().toString());

        // 3. Ask a question about the indexed code
        // We use try-catch because external services might not be available during standard build
        try {
            String question = "What methods does the Calculator class have?";
            System.out.println("Asking question: " + question);
            String response = assistant.chat(question);

            System.out.println("AI Response: " + response);

            // 4. Verify the response
            Assertions.assertNotNull(response, "Response should not be null");
            Assertions.assertFalse(response.isBlank(), "Response should not be empty");
        } catch (Exception e) {
            System.out.println("Could not complete integration test (Ollama/Qdrant likely down): " + e.getMessage());
        }
    }
}
