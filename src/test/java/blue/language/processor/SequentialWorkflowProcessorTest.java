package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyOnEventContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.TestEvent;
import blue.language.utils.Properties;
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
        Node document = operationWorkflowDocument(null, "ownerChannel");

        Node initialized = blue.initializeDocument(document).document();
        String documentBlueId = storedDocumentBlueId(initialized);
        Node event = operationRequestEvent(blue, "increment", 1, false, documentBlueId, "owner-42");
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("1"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationSkipsWhenOperationKeyDiffers() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocument(null, "ownerChannel")).document();
        Node event = operationRequestEvent(blue, "otherOperation", 5, true, null, "owner-42");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("0"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationSkipsWhenRequestTypeMismatches() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocument(null, "ownerChannel")).document();
        Node event = operationRequestEvent(blue, "increment", "oops", true, null, "owner-42");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("0"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationSkipsWhenPinnedDocumentDiffers() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocument(null, "ownerChannel")).document();
        Node event = operationRequestEvent(blue, "increment", 5, false, "stale-document-id", "owner-42");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("0"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationSkipsWhenHandlerAndOperationChannelsConflict() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocument("ownerChannel", "otherChannel")).document();
        Node event = operationRequestEvent(blue, "increment", 5, true, null, "owner-42");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("0"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void javaScriptCodeStepCanApplyChangesetAndEmitEvents() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyOnEventContractProcessor());

        Node document = blue.yamlToNode("name: JavaScript Step Doc\n" +
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
                "          blueId: Conversation/JavaScript Code\n" +
                "        code: \"({ changeset: [{ op: 'ADD', path: '/jsValue', val: 9 }], emit: { kind: 'js' } })\"\n" +
                "  observeJsEmission:\n" +
                "    channel: triggered\n" +
                "    type:\n" +
                "      blueId: SetPropertyOnEvent\n" +
                "    expectedKind: js\n" +
                "    propertyKey: /jsObserved\n" +
                "    propertyValue: 1\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-3").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("9"), result.document().getProperties().get("jsValue").getValue());
        assertEquals(new BigInteger("1"), result.document().getProperties().get("jsObserved").getValue());
    }

    @Test
    void updateDocumentStepResolvesExpressionValues() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: Update Expression Doc\n" +
                "base: 2\n" +
                "contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  workflow:\n" +
                "    channel: testChannel\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: ADD\n" +
                "            path: /computed\n" +
                "            val: \"${event.count.value + document('/base')}\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.yamlToNode("type:\n" +
                "  blueId: TestEvent\n" +
                "eventId: evt-4\n" +
                "kind: TestEvent\n" +
                "count: 3\n");
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("5"), result.document().getProperties().get("computed").getValue());
    }

    @Test
    void triggerEventStepResolvesEventExpressions() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyOnEventContractProcessor());

        Node document = blue.yamlToNode("name: Trigger Expression Doc\n" +
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
                "          blueId: Conversation/Trigger Event\n" +
                "        event:\n" +
                "          kind: \"${event.kind.value}\"\n" +
                "  observer:\n" +
                "    channel: triggered\n" +
                "    type:\n" +
                "      blueId: SetPropertyOnEvent\n" +
                "    expectedKind: TestEvent\n" +
                "    propertyKey: /triggerResolved\n" +
                "    propertyValue: 1\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-5").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("1"), result.document().getProperties().get("triggerResolved").getValue());
    }

    private Node operationWorkflowDocument(String handlerChannel, String operationChannel) {
        String handlerChannelYaml = handlerChannel != null
                ? "    channel: " + handlerChannel + "\n"
                : "";
        String operationChannelYaml = operationChannel != null
                ? "    channel: " + operationChannel + "\n"
                : "";
        Blue blue = new Blue();
        return blue.yamlToNode("name: Workflow Operation Doc\n" +
                "counter: 0\n" +
                "contracts:\n" +
                "  ownerChannel:\n" +
                "    type:\n" +
                "      blueId: Conversation/Timeline Channel\n" +
                "    timelineId: owner-42\n" +
                "  otherChannel:\n" +
                "    type:\n" +
                "      blueId: Conversation/Timeline Channel\n" +
                "    timelineId: other-42\n" +
                "  increment:\n" +
                "    type:\n" +
                "      blueId: Conversation/Operation\n" +
                operationChannelYaml +
                "    request:\n" +
                "      type: Integer\n" +
                "  operationWorkflow:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow Operation\n" +
                handlerChannelYaml +
                "    operation: increment\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /counter\n" +
                "            val: \"${event.message.request.value}\"\n");
    }

    private Node operationRequestEvent(Blue blue,
                                       String operation,
                                       Object request,
                                       boolean allowNewerVersion,
                                       String documentBlueId,
                                       String timelineId) {
        Node message = new Node().type(new Node().blueId("Conversation/Operation Request"))
                .properties("operation", new Node().value(operation))
                .properties("request", requestNode(request))
                .properties("allowNewerVersion", new Node().value(allowNewerVersion));
        if (documentBlueId != null) {
            message.properties("document", new Node().properties("blueId", new Node().value(documentBlueId)));
        }

        return new Node().type(new Node().blueId("Conversation/Timeline Entry"))
                .properties("eventId", new Node().value("evt-op"))
                .properties("timeline", new Node().properties("timelineId", new Node().value(timelineId)))
                .properties("message", message);
    }

    private Node requestNode(Object request) {
        Node node = new Node().value(request);
        if (request instanceof Number) {
            node.type(new Node().blueId(Properties.INTEGER_TYPE_BLUE_ID));
        } else if (request instanceof String) {
            node.type(new Node().blueId(Properties.TEXT_TYPE_BLUE_ID));
        } else if (request instanceof Boolean) {
            node.type(new Node().blueId(Properties.BOOLEAN_TYPE_BLUE_ID));
        }
        return node;
    }

    private String storedDocumentBlueId(Node document) {
        return String.valueOf(document.getProperties()
                .get("contracts")
                .getProperties()
                .get("initialized")
                .getProperties()
                .get("documentId")
                .getValue());
    }
}
