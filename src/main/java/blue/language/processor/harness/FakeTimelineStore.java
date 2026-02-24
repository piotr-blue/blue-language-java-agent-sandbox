package blue.language.processor.harness;

import blue.language.model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory timeline entry storage for harness scenarios.
 */
public final class FakeTimelineStore {

    private final Map<String, List<Node>> entriesByTimelineId = new LinkedHashMap<String, List<Node>>();

    public void append(String timelineId, Node entry) {
        String normalizedTimelineId = requireText(timelineId, "timelineId");
        Objects.requireNonNull(entry, "entry");
        List<Node> entries = entriesByTimelineId.get(normalizedTimelineId);
        if (entries == null) {
            entries = new ArrayList<Node>();
            entriesByTimelineId.put(normalizedTimelineId, entries);
        }
        entries.add(entry.clone());
    }

    public List<Node> entries(String timelineId) {
        String normalizedTimelineId = requireText(timelineId, "timelineId");
        List<Node> entries = entriesByTimelineId.get(normalizedTimelineId);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(cloneList(entries));
    }

    public Map<String, List<Node>> allEntries() {
        Map<String, List<Node>> copy = new LinkedHashMap<String, List<Node>>();
        for (Map.Entry<String, List<Node>> entry : entriesByTimelineId.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(cloneList(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public boolean hasTimeline(String timelineId) {
        String normalizedTimelineId = requireText(timelineId, "timelineId");
        return entriesByTimelineId.containsKey(normalizedTimelineId);
    }

    private List<Node> cloneList(List<Node> values) {
        List<Node> cloned = new ArrayList<Node>();
        for (Node value : values) {
            cloned.add(value != null ? value.clone() : null);
        }
        return cloned;
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
