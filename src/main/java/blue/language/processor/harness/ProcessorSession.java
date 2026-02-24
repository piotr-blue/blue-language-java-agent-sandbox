package blue.language.processor.harness;

import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import blue.language.processor.DocumentProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Mutable runtime session around {@link DocumentProcessor}.
 */
public final class ProcessorSession {

    private final DocumentProcessor processor;
    private final Deque<Node> inboundEvents = new ArrayDeque<Node>();
    private final List<Node> emittedEvents = new ArrayList<Node>();
    private final List<Long> gasPerRun = new ArrayList<Long>();

    private Node currentDocument;
    private boolean initialized;

    ProcessorSession(DocumentProcessor processor, Node initialDocument) {
        this.processor = Objects.requireNonNull(processor, "processor");
        this.currentDocument = Objects.requireNonNull(initialDocument, "initialDocument").clone();
        this.initialized = processor.isInitialized(this.currentDocument);
    }

    public Node document() {
        return currentDocument.clone();
    }

    public boolean initialized() {
        return initialized;
    }

    public int pendingInboundEvents() {
        return inboundEvents.size();
    }

    public List<Node> emittedEvents() {
        List<Node> cloned = new ArrayList<Node>();
        for (Node event : emittedEvents) {
            cloned.add(event != null ? event.clone() : null);
        }
        return Collections.unmodifiableList(cloned);
    }

    public List<Long> gasPerRun() {
        return Collections.unmodifiableList(new ArrayList<Long>(gasPerRun));
    }

    public DocumentProcessingResult init() {
        if (initialized) {
            throw new IllegalStateException("Session document is already initialized");
        }
        DocumentProcessingResult result = processor.initializeDocument(currentDocument.clone());
        applyResult(result);
        initialized = true;
        return result;
    }

    public ProcessorSession pushEvent(Node event) {
        inboundEvents.addLast(Objects.requireNonNull(event, "event").clone());
        return this;
    }

    public boolean runOne() {
        if (!initialized) {
            throw new IllegalStateException("Session document is not initialized");
        }
        Node next = inboundEvents.pollFirst();
        if (next == null) {
            return false;
        }
        DocumentProcessingResult result = processor.processDocument(currentDocument.clone(), next);
        applyResult(result);
        return true;
    }

    public int runUntilIdle() {
        int processedCount = 0;
        while (runOne()) {
            processedCount++;
        }
        return processedCount;
    }

    private void applyResult(DocumentProcessingResult result) {
        Objects.requireNonNull(result, "result");
        currentDocument = result.document().clone();
        gasPerRun.add(Long.valueOf(result.totalGas()));
        for (Node event : result.triggeredEvents()) {
            emittedEvents.add(event != null ? event.clone() : null);
        }
    }
}
