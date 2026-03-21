# AI Code Assistant

A smart CLI assistant for Java code analysis built with Quarkus, Ollama, and Qdrant. This tool indexes your Java project's methods into a vector database and allows you to ask questions about your codebase using a Large Language Model (LLM).

## Features

- **Project Indexing**: Automatically parses Java files and extracts method declarations using JavaParser.
- **Vector Search**: Generates embeddings for code snippets and stores them in Qdrant for efficient similarity search.
- **Context-Aware Chat**: Uses RAG (Retrieval-Augmented Generation) to provide accurate answers about your code by retrieving relevant context before querying the LLM.
- **CLI Interface**: Easy-to-use command-line interface powered by Picocli.

## Tech Stack

- **Framework**: [Quarkus](https://quarkus.io/)
- **CLI**: [Picocli](https://picocli.info/)
- **LLM Engine**: [Ollama](https://ollama.com/) (Models: `qwen2.5-coder:3b` for chat, `nomic-embed-text` for embeddings)
- **Vector Database**: [Qdrant](https://qdrant.tech/)
- **Java Parser**: [JavaParser](https://javaparser.org/)

## Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Ollama** (running locally or in a container)

## Getting Started

### 1. Start Infrastructure

Use Docker Compose to start Qdrant and Ollama:

```bash
docker-compose up -d
```

### 2. Prepare AI Models

Pull the required models in Ollama:

```bash
docker exec -it ollama ollama pull nomic-embed-text
docker exec -it ollama ollama pull qwen2.5-coder:3b
```

### 3. Build the Project

Build the Quarkus application:

```bash
./mvnw clean package
```

### 4. Run the Assistant

Run the application and point it to the Java project you want to index:

```bash
java -jar target/quarkus-app/quarkus-run.jar --path /path/to/your/java-project
```

Or using Maven:

```bash
./mvnw quarkus:dev -Dquarkus.args="--path /path/to/your/java-project"
```

## Usage

Once started, the assistant will index all Java methods in the specified directory. After indexing, you can ask questions like:

- "What does the `calculateTotal` method do?"
- "Are there any methods that handle user authentication?"
- "Explain how the database connection is initialized."

Type `exit` to quit the application.

## Configuration

Settings can be adjusted in `src/main/resources/application.properties`:

- `ollama.chat-model.model-id`: The LLM used for answering questions.
- `ollama.embedding-model.model-id`: The model used for generating vector embeddings.
- `quarkus.rest-client.ollama-api.url`: URL to your Ollama instance.
- `qdrant.host` / `qdrant.port`: Connection details for Qdrant.
