package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void allowsLeadingZeroPropertyKeyWhenParentIsObject() {
        Blue blue = new Blue();
        Node node = new Node()
                .name("Guarded")
                .properties("box", new Node()
                        .properties("01", new Node().value("before")));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/box/01", new Node().value("after")));
        ResolvedSnapshotV2 committed = workingDocument.commit();

        Node box = committed.resolvedRoot().toNode().getProperties().get("box");
        assertTrue(box.getProperties().containsKey("01"));
        assertEquals("after", box.getProperties().get("01").getValue());
    }

    @Test
    void prefersNumericPropertyOverArrayIndexWhenParentContainsBoth() {
        Blue blue = new Blue();
        Node node = new Node()
                .name("Guarded")
                .properties("mixed", new Node()
                        .items(new Node().value("item-zero"))
                        .properties("0", new Node().value("property-zero")));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/mixed/0", new Node().value("property-updated")));
        ResolvedSnapshotV2 committed = workingDocument.commit();

        Node mixed = committed.resolvedRoot().toNode().getProperties().get("mixed");
        assertEquals("property-updated", mixed.getProperties().get("0").getValue());
        assertEquals("item-zero", mixed.getItems().get(0).getValue());
    }

    @Test
    void allowsEscapedPropertyPointerSegments() {
        Blue blue = new Blue();
        Node node = new Node()
                .name("Guarded")
                .properties("a/b", new Node().value("before"));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/a~1b", new Node().value("after")));
        ResolvedSnapshotV2 committed = workingDocument.commit();

        assertEquals("after", committed.resolvedRoot().toNode().getProperties().get("a/b").getValue());
    }

    @Test
    void allowsTrailingEmptyPropertySegmentWhenKeyExists() {
        Blue blue = new Blue();
        Node node = new Node()
                .name("Guarded")
                .properties("scope", new Node().properties("", new Node().value("before")));
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/scope/", new Node().value("after")));
        ResolvedSnapshotV2 committed = workingDocument.commit();

        assertEquals("after",
                committed.resolvedRoot().toNode().getProperties().get("scope").getProperties().get("").getValue());
    }

    @Test
    void rejectsPatchPathWithoutLeadingSlash() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode("name: Guarded\nx: 1\n");
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(IllegalArgumentException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("x", new Node().value(2))));
    }

    @Test
    void rejectsEmptyPatchPath() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode("name: Guarded\nx: 1\n");
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(node);

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        assertThrows(IllegalArgumentException.class,
                () -> workingDocument.applyPatch(JsonPatch.replace("", new Node().value(2))));
    }
}
