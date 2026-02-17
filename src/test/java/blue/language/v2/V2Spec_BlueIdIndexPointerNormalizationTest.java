package blue.language.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V2Spec_BlueIdIndexPointerNormalizationTest {

    @Test
    void blueIdIndexTreatsNullAndEmptyPointersAsRoot() {
        Blue blue = new Blue();
        Node authoring = new Node().name("Root").properties("x", new Node().value(1));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);

        assertEquals(snapshot.rootBlueId(), snapshot.blueIdsByPointer().blueIdAt(null));
        assertEquals(snapshot.rootBlueId(), snapshot.blueIdsByPointer().blueIdAt(""));
        assertEquals(snapshot.rootBlueId(), snapshot.blueIdsByPointer().blueIdAt("/"));
    }

    @Test
    void blueIdIndexRejectsNonPointerLookupPaths() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(new Node().name("Root"));

        assertThrows(IllegalArgumentException.class, () -> snapshot.blueIdsByPointer().blueIdAt("root"));
    }

    @Test
    void blueIdIndexKeepsTrailingEmptySegmentsDistinct() {
        Blue blue = new Blue();
        Node authoring = new Node()
                .name("Root")
                .properties("scope", new Node().properties("", new Node().value("empty-key")));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);

        String scopeBlueId = snapshot.blueIdsByPointer().blueIdAt("/scope");
        String emptyChildBlueId = snapshot.blueIdsByPointer().blueIdAt("/scope/");

        assertEquals(BlueIdCalculatorV2.rehashPath(snapshot.canonicalRoot().toNode(), "/scope"), scopeBlueId);
        assertEquals(BlueIdCalculatorV2.rehashPath(snapshot.canonicalRoot().toNode(), "/scope/"), emptyChildBlueId);
    }
}
