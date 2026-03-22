package org.aicodeassistant;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MainAppTest {

    @Inject
    CodeAssistant assistant;

    @Inject
    CodeIndexer indexer;

    @Test
    void testBeansAreInjected() {
        Assertions.assertNotNull(assistant, "CodeAssistant should be injected");
        Assertions.assertNotNull(indexer, "CodeIndexer should be injected");
    }
}