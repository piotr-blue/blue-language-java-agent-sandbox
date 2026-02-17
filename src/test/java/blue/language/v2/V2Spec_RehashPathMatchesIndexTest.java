package blue.language.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V2Spec_RehashPathMatchesIndexTest {

    @Test
    void rehashPathMatchesSnapshotPointerIndexForAllPointers() {
        Blue blue = new Blue();
        Node authoring = new Node()
                .name("Root")
                .type(new Node().name("CoreType"))
                .items(
                        new Node().name("First"),
                        new Node().name("Second")
                )
                .properties("a/b", new Node().value("slash"))
                .properties("type", new Node().value("property-overrides-type-segment"));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        for (Map.Entry<String, String> entry : snapshot.blueIdsByPointer().asMap().entrySet()) {
            assertEquals(
                    entry.getValue(),
                    BlueIdCalculatorV2.rehashPath(canonicalRoot, entry.getKey()),
                    "Pointer mismatch for " + entry.getKey()
            );
        }
    }
}
