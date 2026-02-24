package blue.language.processor.harness;

import blue.language.model.Node;
import blue.language.processor.DocumentProcessor;

import java.util.Objects;

/**
 * Entry point for running processor sessions in tests and sandboxes.
 */
public final class ProcessorHarness {

    private final DocumentProcessor processor;

    public ProcessorHarness() {
        this(new DocumentProcessor());
    }

    public ProcessorHarness(DocumentProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public ProcessorSession start(Node document) {
        Objects.requireNonNull(document, "document");
        return new ProcessorSession(processor, document.clone());
    }
}
