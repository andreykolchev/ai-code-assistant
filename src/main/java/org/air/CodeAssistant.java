package org.air;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.function.Supplier;

@RegisterAiService(retrievalAugmentor = CodeAssistant.CodeRetrievalAugmentor.CodeRetrievalAugmentorSupplier.class)
public interface CodeAssistant {

    @SystemMessage("""
            You are a code assistant. Your knowledge base contains methods from a Java project.
            Answer concisely and provide code examples where appropriate.
            """)
    @UserMessage("Question about the project: {question}")
    String chat(String question);

    @ApplicationScoped
    class CodeRetrievalAugmentor {

        @Produces
        @ApplicationScoped
        public RetrievalAugmentor create(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
            return DefaultRetrievalAugmentor.builder()
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(3)
                            .build())
                    .build();
        }

        public static class CodeRetrievalAugmentorSupplier implements Supplier<RetrievalAugmentor> {
            @Override
            public RetrievalAugmentor get() {
                return io.quarkus.arc.Arc.container().instance(RetrievalAugmentor.class).get();
            }
        }
    }
}
