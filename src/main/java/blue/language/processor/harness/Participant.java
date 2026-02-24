package blue.language.processor.harness;

import java.util.Objects;

/**
 * Participant identity and mapped timeline id for harness sessions.
 */
public final class Participant {

    private final String key;
    private final String timelineId;

    public Participant(String key, String timelineId) {
        this.key = requireText(key, "key");
        this.timelineId = requireText(timelineId, "timelineId");
    }

    public String key() {
        return key;
    }

    public String timelineId() {
        return timelineId;
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
