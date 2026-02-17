package blue.language.blueid.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MapBlueIdIndex implements BlueIdIndex {

    private static final MapBlueIdIndex EMPTY = new MapBlueIdIndex(Collections.<String, String>emptyMap());

    private final Map<String, String> index;

    private MapBlueIdIndex(Map<String, String> index) {
        this.index = Collections.unmodifiableMap(new LinkedHashMap<String, String>(index));
    }

    public static MapBlueIdIndex empty() {
        return EMPTY;
    }

    public static MapBlueIdIndex from(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return empty();
        }
        Map<String, String> normalized = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String normalizedKey = normalizePointer(entry.getKey());
            if (normalized.containsKey(normalizedKey)) {
                throw new IllegalArgumentException("Duplicate normalized pointer key: " + normalizedKey);
            }
            normalized.put(normalizedKey, entry.getValue());
        }
        return new MapBlueIdIndex(normalized);
    }

    @Override
    public String blueIdAt(String jsonPointer) {
        return index.get(normalizePointer(jsonPointer));
    }

    @Override
    public Map<String, String> asMap() {
        return index;
    }

    private static String normalizePointer(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return "/";
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("Invalid JSON pointer: " + pointer);
        }
        validatePointerEscapes(pointer);
        return pointer;
    }

    private static void validatePointerEscapes(String pointer) {
        for (int i = 1; i < pointer.length(); i++) {
            char c = pointer.charAt(i);
            if (c != '~') {
                continue;
            }
            if (i + 1 >= pointer.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
            char next = pointer.charAt(++i);
            if (next != '0' && next != '1') {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
        }
    }
}
