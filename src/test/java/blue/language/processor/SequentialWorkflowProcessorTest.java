package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyOnEventContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.TestEvent;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SequentialWorkflowProcessorTest {

    @Test
    void sequentialWorkflowExecutesUpdateAndTriggerEventSteps() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyOnEventContractProcessor());

        Node document = blue.yamlToNode("name: Workflow Doc\n" +
                "contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  triggered:\n" +
                "    type:\n" +
                "      blueId: TriggeredEventChannel\n" +
                "  workflow:\n" +
                "    channel: testChannel\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: ADD\n" +
                "            path: /count\n" +
                "            val: 3\n" +
                "      - type:\n" +
                "          blueId: Conversation/Trigger Event\n" +
                "        event:\n" +
                "          type:\n" +
                "            blueId: TriggeredFromWorkflow\n" +
                "          kind: emitted\n" +
                "  observeTrigger:\n" +
                "    channel: triggered\n" +
                "    type:\n" +
                "      blueId: SetPropertyOnEvent\n" +
                "    expectedKind: emitted\n" +
                "    propertyKey: /emitted\n" +
                "    propertyValue: 1\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-1").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        Node processed = result.document();
        assertEquals(new BigInteger("3"), processed.getProperties().get("count").getValue());
        assertEquals(new BigInteger("1"), processed.getProperties().get("emitted").getValue());
        assertNotNull(result.triggeredEvents().stream()
                .filter(node -> node.getProperties() != null)
                .filter(node -> node.getProperties().get("kind") != null)
                .filter(node -> "emitted".equals(node.getProperties().get("kind").getValue()))
                .findFirst()
                .orElse(null));
    }

    @Test
    void sequentialWorkflowOperationDerivesChannelFromOperationMarker() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: Workflow Operation Doc\n" +
                "contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  operationMarker:\n" +
                "    type:\n" +
                "      blueId: Conversation/Operation\n" +
                "    channel: testChannel\n" +
                "  operationWorkflow:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow Operation\n" +
                "    operation: operationMarker\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: ADD\n" +
                "            path: /operationRan\n" +
                "            val: 1\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-2").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("1"), result.document().getProperties().get("operationRan").getValue());
    }
}
