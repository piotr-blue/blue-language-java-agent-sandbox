package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.PatchReport;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class V2Usage_ReadResolvePatchCommitTest {

    @Test
    void readsResolvesPatchesAndCommits() {
        Blue blue = new Blue();
        Node authoring = blue.yamlToNode(
                "name: Counter\n" +
                        "counter: 0\n"
        );

        ResolvedSnapshotV2 initial = blue.resolveToSnapshotV2(authoring);
        assertNotNull(initial);
        assertNotNull(initial.rootBlueId());
        assertNotNull(initial.canonicalRoot());
        assertNotNull(initial.resolvedRoot());
        assertNotNull(initial.blueIdsByPointer());
        assertFalse(initial.blueIdsByPointer().asMap().isEmpty());

        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, initial);
        PatchReport report = workingDocument.applyPatch(JsonPatch.replace("/counter", new Node().value(1)));
        assertNotNull(report);
        assertTrue(report.changed());
        assertEquals("/counter", report.appliedPaths().get(0));

        ResolvedSnapshotV2 next = workingDocument.commit();
        assertNotNull(next);
        assertNotEquals(initial.rootBlueId(), next.rootBlueId());
        assertEquals(1, next.resolvedRoot().toNode().getAsInteger("/counter/value"));
    }
}
