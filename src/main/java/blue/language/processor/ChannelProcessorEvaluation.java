package blue.language.processor;

import blue.language.model.Node;

/**
 * Channel evaluation outcome with optional channelized event details.
 */
public final class ChannelProcessorEvaluation {
    private final boolean matches;
    private final String eventId;
    private final Node eventNode;

    private ChannelProcessorEvaluation(boolean matches, String eventId, Node eventNode) {
        this.matches = matches;
        this.eventId = eventId;
        this.eventNode = eventNode;
    }

    public boolean matches() {
        return matches;
    }

    public String eventId() {
        return eventId;
    }

    public Node eventNode() {
        return eventNode;
    }

    public static ChannelProcessorEvaluation noMatch() {
        return new ChannelProcessorEvaluation(false, null, null);
    }

    public static ChannelProcessorEvaluation matched(String eventId, Node eventNode) {
        return new ChannelProcessorEvaluation(true, eventId, eventNode);
    }
}
