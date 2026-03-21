package org.air;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;

@ApplicationScoped
public class CodeIndexer {

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    public void indexDirectory(String path) throws Exception {
        File root = new File(path);
        if (!root.exists()) {
            System.err.println("Path does not exist: " + path);
            return;
        }
        processFile(root);
        System.out.println("Indexing completed!");
    }

    private void processFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    processFile(child);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            try {
                var cu = StaticJavaParser.parse(file);
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    String content = method.toString();
                    TextSegment segment = TextSegment.from(content);
                    embeddingStore.add(embeddingModel.embed(segment).content(), segment);
                });
            } catch (Exception e) {
                System.err.println("Error parsing file " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }
    }
}