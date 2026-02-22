package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScopeExecutorDerivedChannelParityTest {

    @Test
    void runsLifecycleHandlersForDerivedLifecycleChannels() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Derived Lifecycle Doc\n" +
                "observed: 0\n" +
                "contracts:\n" +
                "  lifecycle:\n" +
                "    type:\n" +
                "      blueId: Derived/Lifecycle Event Channel\n" +
                "      type:\n" +
                "        blueId: Core/Lifecycle Event Channel\n" +
                "  onInit:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    channel: lifecycle\n" +
                "    event:\n" +
                "      type:\n" +
                "        blueId: Core/Document Processing Initiated\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /observed\n" +
                "            val: 1\n");

        DocumentProcessingResult initialized = blue.initializeDocument(document);
        assertFalse(initialized.capabilityFailure(), "Initialization should succeed");
        assertEquals(new BigInteger("1"), ProcessorEngine.nodeAt(initialized.document(), "/observed").getValue());
    }

    @Test
    void routesPatchUpdatesToDerivedDocumentUpdateChannels() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Derived Update Doc\n" +
                "observed: 0\n" +
                "contracts:\n" +
                "  lifecycle:\n" +
                "    type:\n" +
                "      blueId: Core/Lifecycle Event Channel\n" +
                "  seed:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    channel: lifecycle\n" +
                "    event:\n" +
                "      type:\n" +
                "        blueId: Core/Document Processing Initiated\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /foo\n" +
                "            val: 1\n" +
                "  updates:\n" +
                "    type:\n" +
                "      blueId: Derived/Document Update Channel\n" +
                "      type:\n" +
                "        blueId: Core/Document Update Channel\n" +
                "    path: /foo\n" +
                "  onUpdate:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    channel: updates\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /observed\n" +
                "            val: 1\n");

        DocumentProcessingResult initialized = blue.initializeDocument(document);
        assertFalse(initialized.capabilityFailure(), "Initialization should succeed");
        assertNotNull(ProcessorEngine.nodeAt(initialized.document(), "/foo"));
        assertEquals(new BigInteger("1"), ProcessorEngine.nodeAt(initialized.document(), "/foo").getValue());
        assertEquals(new BigInteger("1"), ProcessorEngine.nodeAt(initialized.document(), "/observed").getValue());
    }
}
