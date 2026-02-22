package blue.language.processor.registry.processors;

import blue.language.model.Node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

final class TimelineEventSupport {

    private static final List<String[]> TIMELINE_ID_PATHS = Arrays.<String[]>asList(
            new String[]{"timeline", "timelineId"},
            new String[]{"timelineId"}
    );

    private static final List<String[]> RECENCY_PATHS = Arrays.<String[]>asList(
            new String[]{"sequence"},
            new String[]{"seq"},
            new String[]{"revision"},
            new String[]{"version"},
            new String[]{"index"},
            new String[]{"timeline", "sequence"},
            new String[]{"timeline", "revision"},
            new String[]{"timeline", "index"}
    );

    private TimelineEventSupport() {
    }

    static String timelineId(Node event) {
        for (String[] path : TIMELINE_ID_PATHS) {
            Object value = valueAtPath(event, path);
            if (value instanceof String) {
                String trimmed = ((String) value).trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    static String eventId(Node event) {
        Object eventId = valueAtPath(event, "eventId");
        if (eventId == null) {
            eventId = valueAtPath(event, "id");
        }
        return eventId != null ? String.valueOf(eventId) : null;
    }

    static boolean isNewer(Node current, Node previous) {
        if (current == null || previous == null) {
            return true;
        }
        BigDecimal currentIndex = recencyValue(current);
        BigDecimal previousIndex = recencyValue(previous);
        if (currentIndex == null || previousIndex == null) {
            return true;
        }
        return currentIndex.compareTo(previousIndex) > 0;
    }

    private static BigDecimal recencyValue(Node node) {
        for (String[] path : RECENCY_PATHS) {
            Object value = valueAtPath(node, path);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof BigInteger) {
                return new BigDecimal((BigInteger) value);
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            if (value instanceof String) {
                try {
                    return new BigDecimal(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    // Continue probing known recency fields.
                }
            }
        }
        return null;
    }

    private static Object valueAtPath(Node root, String... path) {
        Node current = root;
        for (String segment : path) {
            if (current == null || current.getProperties() == null) {
                return null;
            }
            current = current.getProperties().get(segment);
        }
        return current != null ? current.getValue() : null;
    }
}
