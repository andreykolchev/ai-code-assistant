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
                    "quarkus.langchain4j.ollama.dev-services.enabled", "false",
                    "quarkus.langchain4j.qdrant.dev-services.enabled", "false",
                    "quarkus.langchain4j.ollama.chat-model.base-url", "http://localhost:11434",
                    "quarkus.langchain4j.ollama.embedding-model.base-url", "http://localhost:11434",
                    "quarkus.langchain4j.qdrant.host", "localhost",
                    "quarkus.langchain4j.qdrant.port", "6334"
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
        String question = "What methods does the Calculator class have and what do they do?";
        System.out.println("Asking question: " + question);
        String response = assistant.chat(question);

        System.out.println("AI Response: " + response);

        // 4. Verify the response
        Assertions.assertNotNull(response, "Response should not be null");
        Assertions.assertFalse(response.isBlank(), "Response should not be empty");
        
        // Since LLM responses can vary, we check for presence of key terms
        String lowerResponse = response.toLowerCase();
        Assertions.assertTrue(lowerResponse.contains("add") || lowerResponse.contains("calculator"), 
                "Response should mention 'add' or 'calculator'. Actual: " + response);
    }
}
