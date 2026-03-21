package org.air;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = CodeAssistant.CodeRetrievalAugmentor.Supplier.class)
public interface CodeAssistant {

    @SystemMessage("""
            You are a code assistant. Your knowledge base contains methods from a Java project.
            Answer concisely and provide code examples where appropriate.
            """)
    @UserMessage("Question about the project: {question}")
    String chat(String question);

    @jakarta.enterprise.context.ApplicationScoped
    class CodeRetrievalAugmentor {

        @jakarta.enterprise.inject.Produces
        @jakarta.enterprise.context.ApplicationScoped
        public dev.langchain4j.rag.RetrievalAugmentor create(dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
                                                             dev.langchain4j.store.embedding.EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore) {
            return dev.langchain4j.rag.DefaultRetrievalAugmentor.builder()
                    .contentRetriever(dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(3)
                            .build())
                    .build();
        }

        public static class Supplier implements java.util.function.Supplier<dev.langchain4j.rag.RetrievalAugmentor> {
            @Override
            public dev.langchain4j.rag.RetrievalAugmentor get() {
                return io.quarkus.arc.Arc.container().instance(dev.langchain4j.rag.RetrievalAugmentor.class).get();
            }
        }
    }
}
