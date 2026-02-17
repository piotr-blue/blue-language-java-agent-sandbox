package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Map<String, Node> properties = canonical.getProperties();
        assertTrue(properties == null || !properties.containsKey("x"));
    }
}
