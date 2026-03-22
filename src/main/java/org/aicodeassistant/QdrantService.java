package org.aicodeassistant;

import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class QdrantService {

    @ConfigProperty(name = "qdrant.host")
    String qdrantHost;

    @ConfigProperty(name = "qdrant.port")
    int qdrantPort;

    @ConfigProperty(name = "qdrant.collection.name")
    String collectionName;

    @ConfigProperty(name = "qdrant.collection.dimension")
    int dimension;

    private io.qdrant.client.QdrantClient qdrantClient;

    @PostConstruct
    void init() throws ExecutionException, InterruptedException {
        qdrantClient = new io.qdrant.client.QdrantClient(QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build());

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

    @PreDestroy
    void cleanup() {
        if (qdrantClient != null) {
            qdrantClient.close();
        }
    }

    public io.qdrant.client.QdrantClient getClient() {
        return qdrantClient;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
