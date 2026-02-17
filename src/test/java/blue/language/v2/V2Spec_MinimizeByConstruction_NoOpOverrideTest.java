package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("Enabled in Phase 5 when minimize-by-construction is implemented")
class V2Spec_MinimizeByConstruction_NoOpOverrideTest {

    @Test
    void replacingWithInheritedValueMustRemoveLocalOverride() {
        Blue blue = new Blue();
        Node authoring = blue.yamlToNode(
                "type:\n" +
                        "  name: Base\n" +
                        "  x: 1\n" +
                        "x: 1\n"
        );

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);
        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/x", new Node().value(1)));

        Node canonical = workingDocument.commit().canonicalRoot().toNode();
        assertFalse(canonical.getProperties().containsKey("x"));
    }
}
