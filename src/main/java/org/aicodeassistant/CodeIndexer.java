package org.aicodeassistant;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.qdrant.client.grpc.Points;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@ApplicationScoped
public class CodeIndexer {

    @Inject
    @RestClient
    OllamaClient ollamaClient;

    @ConfigProperty(name = "ollama.embedding-model.model-id")
    String embeddingModel;

    @Inject
    QdrantService qdrantService;

    private final List<String> indexedMethods = new ArrayList<>();


    public List<String> getIndexedMethods() {
        return indexedMethods;
    }

    public void indexDirectory(String path) throws Exception {
        File root = new File(path);
        if (!root.exists()) {
            System.err.println("Path does not exist: " + path);
            return;
        }

        indexedMethods.clear();
        parseJavaFile(root);

        if (indexedMethods.isEmpty()) {
            System.out.println("No Java methods found to index.");
            return;
        }

        System.out.println("Generating embeddings and indexing " + indexedMethods.size() + " methods...");

        List<Points.PointStruct> points = new ArrayList<>();
        for (String methodCode : indexedMethods) {
            try {
                var response = ollamaClient.getEmbedding(new OllamaClient.EmbeddingRequest(embeddingModel, methodCode));
                if (response != null && response.embedding != null) {
                    List<Float> vector = response.embedding;

                    points.add(Points.PointStruct.newBuilder()
                        .setId(id(UUID.randomUUID()))
                        .setVectors(vectors(vector))
                        .putPayload("content", value(methodCode))
                        .build());
                } else {
                    System.err.println("Warning: Received empty embedding from Ollama for method snippet.");
                }
            } catch (Exception e) {
                System.err.println("Error generating embedding for a method: " + e.getMessage());
                // Continue with next method
            }
        }

        if (!points.isEmpty()) {
            qdrantService.getClient().upsertAsync(qdrantService.getCollectionName(), points).get();
            System.out.println("Indexing completed! Successfully indexed " + points.size() + " methods.");
        } else {
            System.out.println("Indexing failed: No methods were successfully embedded.");
        }
    }

    private void parseJavaFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    parseJavaFile(child);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            try {
                var cu = StaticJavaParser.parse(file);
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    indexedMethods.add(method.toString());
                });
            } catch (Exception e) {
                System.err.println("Error parsing file " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }
    }
}