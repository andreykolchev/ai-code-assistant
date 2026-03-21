package org.air;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

    @ConfigProperty(name = "qdrant.host")
    String qdrantHost;

    @ConfigProperty(name = "qdrant.port")
    int qdrantPort;

    @ConfigProperty(name = "qdrant.collection.name")
    String collectionName;

    @ConfigProperty(name = "qdrant.collection.dimension")
    int dimension;

    private QdrantClient qdrantClient;

    private final List<String> indexedMethods = new ArrayList<>();

    @PostConstruct
    void init() throws ExecutionException, InterruptedException {
        qdrantClient = new QdrantClient(QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build());

        // Check if collection exists, if not create it
        List<String> collections = qdrantClient.listCollectionsAsync().get();
        if (!collections.contains(collectionName)) {
            qdrantClient.createCollectionAsync(collectionName,
                Collections.VectorParams.newBuilder()
                    .setDistance(Collections.Distance.Cosine)
                    .setSize(dimension)
                    .build()
            ).get();
        }
    }

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
            var response = ollamaClient.getEmbedding(new OllamaClient.EmbeddingRequest(embeddingModel, methodCode));
            List<Float> vector = response.embedding;

            points.add(Points.PointStruct.newBuilder()
                .setId(id(UUID.randomUUID()))
                .setVectors(vectors(vector))
                .putPayload("content", value(methodCode))
                .build());
        }

        if (!points.isEmpty()) {
            qdrantClient.upsertAsync(collectionName, points).get();
        }

        System.out.println("Indexing completed! Indexed " + indexedMethods.size() + " methods.");
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