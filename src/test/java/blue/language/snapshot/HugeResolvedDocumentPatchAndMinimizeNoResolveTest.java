package blue.language.snapshot;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.provider.FailOnFetchNodeProvider;
import blue.language.utils.UncheckedObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class HugeResolvedDocumentPatchAndMinimizeNoResolveTest {

    @Test
    void hugeResolvedDocumentCanBePatchedWithoutAnyResolveFetchCalls() {
        Node resolved = UncheckedObjectMapper.JSON_MAPPER.readValue(
                loadFixture("fixtures/counter-webhook.json"), Node.class);

        Blue blue = new Blue(new FailOnFetchNodeProvider());
        ResolvedSnapshot initial = blue.resolveToSnapshot(resolved, SnapshotTrust.BLIND_TRUST_RESOLVED);
        String initialBlueId = initial.rootBlueId();

        WorkingDocument workingDocument = WorkingDocument.forSnapshot(blue, initial);
        assertTimeoutPreemptively(Duration.ofSeconds(15), new Executable() {
            @Override
            public void execute() {
                for (int i = 0; i < 1000; i++) {
                    workingDocument.applyPatch(JsonPatch.replace("/counter/value", new Node().value(BigInteger.valueOf(i))));
                    workingDocument.applyPatch(JsonPatch.replace("/status/value", new Node().value((i % 2 == 0) ? "running" : "paused")));
                }
            }
        });

        ResolvedSnapshot next = workingDocument.commit();
        assertNotNull(next.rootBlueId());
        assertNotNull(next.canonicalRoot());

        String resolvedJson = blue.nodeToJson(next.resolvedRoot().toNode());
        String minimizedJson = blue.nodeToJson(next.canonicalRoot().toNode());
        assertTrue(minimizedJson.length() < resolvedJson.length(),
                "Canonical/minimized transport form should be smaller than resolved form");

        assertTrue(minimizedJson.contains("\"blueId\""),
                "Minimized output should keep blueId references for transport");
        assertNotNull(initialBlueId);
    }

    @Test
    void noOpPatchKeepsSemanticBlueIdStable() {
        Blue blue = new Blue(new FailOnFetchNodeProvider());
        Node resolved = blue.yamlToNode(
                "name: CounterDoc\n" +
                        "counter: 0\n"
        );

        ResolvedSnapshot snapshot = blue.resolveToSnapshot(resolved, SnapshotTrust.BLIND_TRUST_RESOLVED);
        String initialBlueId = snapshot.rootBlueId();

        Node existingCounter = snapshot.resolvedRoot().toNode().getProperties().get("counter").clone();
        WorkingDocument noOp = WorkingDocument.forSnapshot(blue, snapshot);
        noOp.applyPatch(JsonPatch.replace("/counter", existingCounter));
        ResolvedSnapshot noOpResult = noOp.commit();
        assertEquals(initialBlueId, noOpResult.rootBlueId(),
                "No-op patch should not change semantic root blueId");
    }

    @Test
    void constrainedTypeChainGeneralizesUpwardWhenPatchViolatesInvariant() {
        Blue blue = new Blue(new FailOnFetchNodeProvider());
        Node resolved = blue.yamlToNode(
                "name: GeneralizationDoc\n" +
                        "item:\n" +
                        "  type:\n" +
                        "    name: ConstrainedC\n" +
                        "    type:\n" +
                        "      name: ConstrainedB\n" +
                        "      type:\n" +
                        "        name: ConstrainedA\n" +
                        "        x:\n" +
                        "          type: Integer\n" +
                        "      x: 1\n" +
                        "    x: 1\n" +
                        "    y:\n" +
                        "      type: Integer\n" +
                        "  x: 1\n" +
                        "  y: 9\n"
        );

        ResolvedSnapshot snapshot = blue.resolveToSnapshot(resolved, SnapshotTrust.BLIND_TRUST_RESOLVED);
        WorkingDocument workingDocument = WorkingDocument.forSnapshot(blue, snapshot);
        PatchReport report = workingDocument.applyPatch(JsonPatch.replace("/item/x", new Node().value(2)));
        ResolvedSnapshot committed = workingDocument.commit();

        assertTrue(report.generalizationReport().hasGeneralizations(),
                "Patch violating constrained subtype must trigger generalization");
        assertEquals("ConstrainedA",
                committed.resolvedRoot().toNode().getAsText("/item/type/name"));
    }

    private String loadFixture(String path) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(inputStream, "Missing fixture: " + path);
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }
}
