package blue.language.processor.harness;

import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import blue.language.processor.DocumentProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable runtime session around {@link DocumentProcessor}.
 */
public final class ProcessorSession {

    private final DocumentProcessor processor;
    private final Deque<Node> inboundEvents = new ArrayDeque<Node>();
    private final List<Node> emittedEvents = new ArrayList<Node>();
    private final List<Long> gasPerRun = new ArrayList<Long>();
    private final Map<String, Participant> participantsByKey = new LinkedHashMap<String, Participant>();
    private final FakeTimelineStore timelineStore = new FakeTimelineStore();

    private Node currentDocument;
    private boolean initialized;
    private long generatedEventCounter = 1L;

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

    public ProcessorSession registerParticipant(String participantKey, String timelineId) {
        Participant participant = new Participant(participantKey, timelineId);
        participantsByKey.put(participant.key(), participant);
        return this;
    }

    public Map<String, Participant> participants() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Participant>(participantsByKey));
    }

    public FakeTimelineStore timelineStore() {
        return timelineStore;
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

    public ProcessorSession initSession() {
        init();
        return this;
    }

    public ProcessorSession pushEvent(Node event) {
        Node clonedEvent = Objects.requireNonNull(event, "event").clone();
        inboundEvents.addLast(clonedEvent);
        appendToTimelineStoreIfEntry(clonedEvent);
        return this;
    }

    public ProcessorSession pushConversationTimelineEntry(String participantKey, Node message) {
        Participant participant = participant(participantKey);
        Node event = EventFactory.conversationTimelineEntry(
                participant.timelineId(),
                nextEventId("timeline"),
                Objects.requireNonNull(message, "message"));
        return pushEvent(event);
    }

    public ProcessorSession pushMyOsTimelineEntry(String participantKey, Node message) {
        Participant participant = participant(participantKey);
        Node event = EventFactory.myOsTimelineEntry(
                participant.timelineId(),
                nextEventId("myos"),
                Objects.requireNonNull(message, "message"));
        return pushEvent(event);
    }

    public ProcessorSession callOperation(String participantKey, String operation, Object requestPayload) {
        Participant participant = participant(participantKey);
        Node request = toNode(requestPayload);
        Node event = EventFactory.conversationOperationRequestEntry(
                participant.timelineId(),
                nextEventId("operation"),
                operation,
                request);
        return pushEvent(event);
    }

    public ProcessorSession callOperation(String participantKey, String operation) {
        return callOperation(participantKey, operation, new Node().properties(new LinkedHashMap<String, Node>()));
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

    private Participant participant(String participantKey) {
        Objects.requireNonNull(participantKey, "participantKey");
        Participant participant = participantsByKey.get(participantKey.trim());
        if (participant == null) {
            throw new IllegalStateException("Participant is not registered: " + participantKey);
        }
        return participant;
    }

    private void appendToTimelineStoreIfEntry(Node event) {
        String timelineId = EventFactory.timelineId(event);
        if (timelineId == null) {
            return;
        }
        timelineStore.append(timelineId, event);
    }

    private String nextEventId(String prefix) {
        String normalizedPrefix = prefix == null || prefix.trim().isEmpty() ? "evt" : prefix.trim();
        String eventId = normalizedPrefix + "-" + generatedEventCounter;
        generatedEventCounter++;
        return eventId;
    }

    private Node toNode(Object payload) {
        if (payload instanceof Node) {
            return ((Node) payload).clone();
        }
        return new Node().value(payload);
    }
}
