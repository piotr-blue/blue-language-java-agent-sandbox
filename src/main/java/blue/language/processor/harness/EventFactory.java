package blue.language.processor.harness;

import blue.language.model.Node;

import java.util.Objects;

/**
 * Canonical event builders for processor harness scenarios.
 */
public final class EventFactory {

    private static final String CONVERSATION_TIMELINE_ENTRY = "Conversation/Timeline Entry";
    private static final String MYOS_TIMELINE_ENTRY = "MyOS/MyOS Timeline Entry";
    private static final String OPERATION_REQUEST = "Conversation/Operation Request";

    private EventFactory() {
    }

    public static Node conversationTimelineEntry(String timelineId, String eventId, Node message) {
        return timelineEntry(CONVERSATION_TIMELINE_ENTRY, timelineId, eventId, message);
    }

    public static Node myOsTimelineEntry(String timelineId, String eventId, Node message) {
        return timelineEntry(MYOS_TIMELINE_ENTRY, timelineId, eventId, message);
    }

    public static Node operationRequestMessage(String operation, Node request) {
        String normalizedOperation = requireText(operation, "operation");
        return new Node()
                .type(new Node().blueId(OPERATION_REQUEST))
                .properties("operation", new Node().value(normalizedOperation))
                .properties("request", request != null ? request.clone() : new Node().value(null));
    }

    public static Node conversationOperationRequestEntry(String timelineId,
                                                        String eventId,
                                                        String operation,
                                                        Node request) {
        return conversationTimelineEntry(timelineId, eventId, operationRequestMessage(operation, request));
    }

    public static String timelineId(Node event) {
        if (event == null || event.getProperties() == null) {
            return null;
        }
        Node timelineNode = event.getProperties().get("timeline");
        if (timelineNode == null || timelineNode.getProperties() == null) {
            return null;
        }
        Node timelineIdNode = timelineNode.getProperties().get("timelineId");
        if (timelineIdNode == null || timelineIdNode.getValue() == null) {
            return null;
        }
        String value = String.valueOf(timelineIdNode.getValue()).trim();
        return value.isEmpty() ? null : value;
    }

    private static Node timelineEntry(String typeBlueId, String timelineId, String eventId, Node message) {
        String normalizedTimelineId = requireText(timelineId, "timelineId");
        Objects.requireNonNull(message, "message");
        Node event = new Node()
                .type(new Node().blueId(typeBlueId))
                .properties("timeline", new Node().properties("timelineId", new Node().value(normalizedTimelineId)))
                .properties("message", message.clone());
        if (eventId != null && !eventId.trim().isEmpty()) {
            event.properties("eventId", new Node().value(eventId.trim()));
        }
        return event;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
