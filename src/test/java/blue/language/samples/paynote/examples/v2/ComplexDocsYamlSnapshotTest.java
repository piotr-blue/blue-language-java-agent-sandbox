package blue.language.samples.paynote.examples.v2;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexDocsYamlSnapshotTest {

    @Test
    void serializesAllShowcaseDocsToYamlSnapshots() {
        Blue blue = new Blue();
        Map<String, Node> docs = ComplexDocsShowcase.buildAll("2026-02-21T12:00:00Z");

        for (Map.Entry<String, Node> entry : docs.entrySet()) {
            String yaml = blue.nodeToYaml(entry.getValue());
            assertTrue(yaml.contains("type"), "Snapshot missing type for " + entry.getKey());
            assertTrue(yaml.contains("document"), "Snapshot missing document for " + entry.getKey());
            assertTrue(yaml.length() > 80, "Snapshot too small for " + entry.getKey());
        }
    }
}
