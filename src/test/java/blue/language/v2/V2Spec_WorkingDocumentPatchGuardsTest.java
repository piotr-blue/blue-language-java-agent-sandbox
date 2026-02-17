package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V2Spec_WorkingDocumentPatchGuardsTest {

    @Test
    void rejectsMutatingBlueIdDirectly() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode("name: Guarded\nx: 1\n");
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(UnsupportedOperationException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("/blueId", new Node().value("illegal"))));
    }

    @Test
    void rejectsMutatingTypeInternals() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode(
                "name: Guarded\n" +
                        "x:\n" +
                        "  type: Integer\n" +
                        "  value: 1\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(UnsupportedOperationException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("/x/type/blueId", new Node().value("illegal"))));
    }

    @Test
    void allowsReplacingTypeNode() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode(
                "name: Guarded\n" +
                        "x:\n" +
                        "  type: Integer\n" +
                        "  value: 1\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/x/type", new Node().blueId("5WNMiV9Knz63B4dVY5JtMyh3FB4FSGqv7ceScvuapdE1")));
        ResolvedSnapshotV2 committed = workingDocument.commit();

        assertEquals("5WNMiV9Knz63B4dVY5JtMyh3FB4FSGqv7ceScvuapdE1",
                committed.resolvedRoot().toNode().getAsText("/x/type/blueId"));
    }

    @Test
    void rejectsMalformedPointerEscapes() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode("name: Guarded\nx: 1\n");
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(IllegalArgumentException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("/x~", new Node().value("illegal"))));
    }

    @Test
    void rejectsLeadingZeroArrayIndexSegments() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode(
                "name: Guarded\n" +
                        "list:\n" +
                        "  - 1\n" +
                        "  - 2\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(IllegalStateException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("/list/01", new Node().value(99))));
    }
}
