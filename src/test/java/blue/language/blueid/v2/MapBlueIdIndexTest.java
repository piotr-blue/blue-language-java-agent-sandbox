package blue.language.blueid.v2;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapBlueIdIndexTest {

    @Test
    void blueIdAtNormalizesRootPointerLookups() {
        Map<String, String> ids = new LinkedHashMap<String, String>();
        ids.put("/", "root-blue-id");

        MapBlueIdIndex index = MapBlueIdIndex.from(ids);
        assertEquals("root-blue-id", index.blueIdAt("/"));
        assertEquals("root-blue-id", index.blueIdAt(""));
        assertEquals("root-blue-id", index.blueIdAt(null));
    }

    @Test
    void blueIdAtRejectsNonPointerPaths() {
        Map<String, String> ids = new LinkedHashMap<String, String>();
        ids.put("/", "root-blue-id");

        MapBlueIdIndex index = MapBlueIdIndex.from(ids);
        assertThrows(IllegalArgumentException.class, () -> index.blueIdAt("root"));
    }

    @Test
    void blueIdAtKeepsTrailingSegmentsExact() {
        Map<String, String> ids = new LinkedHashMap<String, String>();
        ids.put("/scope", "scope-id");
        ids.put("/scope/", "scope-empty-child-id");

        MapBlueIdIndex index = MapBlueIdIndex.from(ids);
        assertEquals("scope-id", index.blueIdAt("/scope"));
        assertEquals("scope-empty-child-id", index.blueIdAt("/scope/"));
    }
}
