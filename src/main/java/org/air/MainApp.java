package org.air;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import jakarta.inject.Inject;
import java.util.Scanner;

@TopCommand
@Command(name = "code-assistant", mixinStandardHelpOptions = true, version = "1.0",
        description = "Smart assistant for Java code analysis")
public class MainApp implements Runnable {

    @Inject
    CodeIndexer indexer;

    @Inject
    CodeAssistant assistant;

    // Named parameter with short (-p) and long (--path) aliases
    @Option(names = {"-p", "--path"}, description = "Path to the Java project directory for indexing", required = true)
    String projectPath;

    @Override
    public void run() {
        try {
            System.out.println("Starting project indexing at path: " + projectPath);
            indexer.indexDirectory(projectPath);
            System.out.println("Indexing completed successfully!\n");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Ask a question about the code (or type 'exit' to quit): ");
                String question = scanner.nextLine();

                if ("exit".equalsIgnoreCase(question.trim())) {
                    System.out.println("Shutting down. Goodbye!");
                    break;
                }

                String response = assistant.chat(question);
                System.out.println("\nAI: " + response + "\n");
            }
        } catch (Exception e) {
            System.err.println("Execution error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}