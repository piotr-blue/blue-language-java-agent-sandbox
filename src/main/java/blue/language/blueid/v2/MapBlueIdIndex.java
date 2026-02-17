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
        return new MapBlueIdIndex(source);
    }

    @Override
    public String blueIdAt(String jsonPointer) {
        return index.get(normalizePointer(jsonPointer));
    }

    @Override
    public Map<String, String> asMap() {
        return index;
    }

    private String normalizePointer(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return "/";
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("Invalid JSON pointer: " + pointer);
        }
        return pointer;
    }
}
