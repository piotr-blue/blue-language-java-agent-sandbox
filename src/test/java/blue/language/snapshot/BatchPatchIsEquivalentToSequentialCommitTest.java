package blue.language.snapshot;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchPatchIsEquivalentToSequentialCommitTest {

    @Test
    void batchPatchesProduceSameResultAsSequentialApplyPatch() {
        Blue blue = new Blue();
        Node authoring = blue.yamlToNode(
                "name: BatchDoc\n" +
                        "counter: 0\n" +
                        "meta:\n" +
                        "  version: v1\n" +
                        "  tags:\n" +
                        "    - a\n" +
                        "    - b\n"
        );
        ResolvedSnapshot initial = blue.resolveToSnapshot(authoring);

        List<JsonPatch> patches = Arrays.asList(
                JsonPatch.replace("/counter", new Node().value(1)),
                JsonPatch.replace("/meta/version", new Node().value("v2")),
                JsonPatch.add("/meta/tags/-", new Node().value("c")),
                JsonPatch.add("/meta/owner", new Node().value("ops")),
                JsonPatch.remove("/meta/tags/0")
        );

        WorkingDocument sequential = WorkingDocument.forSnapshot(blue, initial);
        for (JsonPatch patch : patches) {
            sequential.applyPatch(patch);
        }
        ResolvedSnapshot sequentialCommitted = sequential.commit();

        WorkingDocument batched = WorkingDocument.forSnapshot(blue, initial);
        PatchReport batchReport = batched.applyPatches(patches);
        ResolvedSnapshot batchedCommitted = batched.commit();

        assertTrue(batchReport.changed());
        assertEquals(5, batchReport.appliedPaths().size());

        assertEquals(sequentialCommitted.rootBlueId(), batchedCommitted.rootBlueId());
        assertEquals(
                blue.nodeToJson(sequentialCommitted.resolvedRoot().toNode()),
                blue.nodeToJson(batchedCommitted.resolvedRoot().toNode())
        );
        assertEquals(
                blue.nodeToJson(sequentialCommitted.canonicalRoot().toNode()),
                blue.nodeToJson(batchedCommitted.canonicalRoot().toNode())
        );
    }

    @Test
    void emptyBatchProducesNoChangeReport() {
        Blue blue = new Blue();
        ResolvedSnapshot initial = blue.resolveToSnapshot(blue.yamlToNode("name: EmptyBatch\nx: 1\n"));

        WorkingDocument workingDocument = WorkingDocument.forSnapshot(blue, initial);
        PatchReport report = workingDocument.applyPatches(Arrays.<JsonPatch>asList());

        assertFalse(report.changed());
        assertTrue(report.appliedPaths().isEmpty());
        assertFalse(report.generalizationReport().hasGeneralizations());
        assertEquals(initial.rootBlueId(), workingDocument.commit().rootBlueId());
    }
}
