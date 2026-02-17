package blue.language.snapshot;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.provider.BasicNodeProvider;
import blue.language.transport.WebhookEnvelope;
import blue.language.utils.UncheckedObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LargeResolvedDocumentPatchPipelineTest {

    @Test
    void preResolvedCoreAndExampleSnapshotsCanBeLoadedAndPatchedWithoutResolve() {
        BasicNodeProvider provider = new BasicNodeProvider();
        String coreSnapshotBlueId = provider.addSingleDocsSemanticResolved(
                "name: Core Channel Resolved\n" +
                        "type:\n" +
                        "  blueId: Core.Channel\n" +
                        "eventType: TestEvent\n"
        );
        String exampleSnapshotBlueId = provider.addSingleDocsSemanticResolved(
                "name: Secure Webhook Resolved\n" +
                        "type:\n" +
                        "  blueId: Example.Channel.Webhook.Secure\n" +
                        "eventType: TestEvent\n" +
                        "route: /events/orders\n" +
                        "signatureAlgorithm: HMAC-SHA256\n"
        );

        Optional<ResolvedSnapshot> coreSnapshot = provider.findSnapshotBySemanticBlueId(coreSnapshotBlueId);
        Optional<ResolvedSnapshot> exampleSnapshot = provider.findSnapshotBySemanticBlueId(exampleSnapshotBlueId);
        assertTrue(coreSnapshot.isPresent());
        assertTrue(exampleSnapshot.isPresent());

        Blue noLookupBlue = new Blue(blueId -> {
            throw new AssertionError("Unexpected provider lookup for blueId: " + blueId);
        });
        WorkingDocument workingDocument = WorkingDocument.forSnapshot(noLookupBlue, exampleSnapshot.get());
        workingDocument.applyPatch(JsonPatch.replace("/route", new Node().value("/events/orders/v2")));
        ResolvedSnapshot committed = workingDocument.commit();

        assertEquals("/events/orders/v2", committed.resolvedRoot().toNode().getAsText("/route/value"));
    }

    @Test
    void largeResolvedDocumentCanBePatchedMinimizedAndTransportedQuicklyWithoutResolve() {
        Node resolvedWebhook = UncheckedObjectMapper.JSON_MAPPER.readValue(
                loadFixture("fixtures/counter-webhook.json"), Node.class);

        Blue bootstrapBlue = new Blue();
        ResolvedSnapshot largeResolvedSnapshot = bootstrapBlue.resolveToSnapshot(
                resolvedWebhook, SnapshotTrust.BLIND_TRUST_RESOLVED);

        Blue noLookupBlue = new Blue(blueId -> {
            throw new AssertionError("Unexpected provider lookup for blueId: " + blueId);
        });

        long startedAtNanos = System.nanoTime();
        ResolvedSnapshot lastCommitted = null;
        for (int i = 0; i < 60; i++) {
            WorkingDocument workingDocument = WorkingDocument.forSnapshot(noLookupBlue, largeResolvedSnapshot);
            workingDocument.applyPatch(JsonPatch.replace("/counter", new Node().value(BigInteger.valueOf(i + 1))));
            workingDocument.applyPatch(JsonPatch.replace("/status", new Node().value(i % 2 == 0 ? "running" : "paused")));
            workingDocument.applyPatch(JsonPatch.replace("/meta/version", new Node().value("v" + i)));
            workingDocument.applyPatch(JsonPatch.add("/meta/flags/-", new Node().value("f" + i)));
            lastCommitted = workingDocument.commit();

            WebhookEnvelope envelope = WebhookEnvelope.fromSnapshot(lastCommitted);
            assertNotNull(envelope.rootBlueId());
            assertNotNull(envelope.canonical());
        }
        long elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L;

        assertNotNull(lastCommitted);
        String resolvedJson = noLookupBlue.nodeToJson(lastCommitted.resolvedRoot().toNode());
        String canonicalJson = noLookupBlue.nodeToJson(lastCommitted.canonicalRoot().toNode());
        assertTrue(canonicalJson.length() < resolvedJson.length(),
                "Transport canonical payload should be minimized compared with resolved payload");
        assertTrue(elapsedMillis < 6000L,
                "Patch + minimize + transport pipeline should stay fast, elapsedMillis=" + elapsedMillis);
    }

    private String loadFixture(String path) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(inputStream, "Missing fixture: " + path);
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }
}
