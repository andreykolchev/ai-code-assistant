package org.air;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.langchain4j.model.embedding.EmbeddingModel;

@QuarkusTest
class MainAppTest {

    @Inject
    CodeAssistant assistant;

    @Inject
    CodeIndexer indexer;

    @Inject
    EmbeddingModel embeddingModel;

    @Test
    void testBeansAreInjected() {
        Assertions.assertNotNull(assistant, "CodeAssistant should be injected");
        Assertions.assertNotNull(indexer, "CodeIndexer should be injected");
        Assertions.assertNotNull(embeddingModel, "EmbeddingModel (Nomic) should be injected");
    }

}