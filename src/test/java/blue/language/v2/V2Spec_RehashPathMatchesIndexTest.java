package blue.language.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V2Spec_RehashPathMatchesIndexTest {

    private static final Node AUTHORING = new Node()
            .name("Root")
            .type(new Node().name("CoreType"))
            .items(
                    new Node().name("First"),
                    new Node().name("Second")
            )
            .properties("a/b", new Node().value("slash"))
            .properties("type", new Node().value("property-overrides-type-segment"));

    @Test
    void rehashPathMatchesSnapshotPointerIndexForAllPointers() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        for (Map.Entry<String, String> entry : snapshot.blueIdsByPointer().asMap().entrySet()) {
            assertEquals(
                    entry.getValue(),
                    BlueIdCalculatorV2.rehashPath(canonicalRoot, entry.getKey()),
                    "Pointer mismatch for " + entry.getKey()
            );
        }
    }

    @Test
    void rehashPathTreatsEmptyPointerAsRoot() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertEquals(
                snapshot.rootBlueId(),
                BlueIdCalculatorV2.rehashPath(canonicalRoot, "")
        );
    }

    @Test
    void rehashPathRejectsInvalidOrMissingPointers() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "type"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/does-not-exist"));
    }
}
