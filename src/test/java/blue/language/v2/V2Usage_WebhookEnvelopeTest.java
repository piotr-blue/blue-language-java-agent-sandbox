package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.transport.v2.WebhookEnvelopeV2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class V2Usage_WebhookEnvelopeTest {

    @Test
    void createsCanonicalWebhookEnvelopeFromSnapshot() {
        Blue blue = new Blue();
        Node doc = blue.yamlToNode(
                "name: EnvelopeDoc\n" +
                        "counter: 1\n"
        );

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        WebhookEnvelopeV2 envelope = WebhookEnvelopeV2.fromSnapshot(snapshot);

        assertEquals(snapshot.rootBlueId(), envelope.rootBlueId());
        assertTrue(envelope.canonical() instanceof Map);
        assertTrue(envelope.blueIdsByPointer().containsKey("/"));
        assertTrue(envelope.bundle().isEmpty());
    }

    @Test
    void canAttachReferenceBundleFromProvider() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: BaseType\n" +
                        "x: 1\n"
        );
        String baseTypeBlueId = provider.getBlueIdByName("BaseType");

        Blue blue = new Blue(provider);
        Node doc = blue.yamlToNode(
                "name: Derived\n" +
                        "type:\n" +
                        "  blueId: " + baseTypeBlueId + "\n"
        );

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        WebhookEnvelopeV2 envelope = WebhookEnvelopeV2.fromSnapshot(snapshot, blue);

        assertFalse(envelope.bundle().isEmpty());
        assertTrue(envelope.bundle().containsKey(baseTypeBlueId));
    }
}
