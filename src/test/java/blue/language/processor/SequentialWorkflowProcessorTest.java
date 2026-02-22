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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static blue.language.utils.Properties.INTEGER_TYPE_BLUE_ID;

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
    void sequentialWorkflowOperationSupportsChangeWorkflowAliases() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocument(
                null,
                "ownerChannel",
                "Conversation/Change Workflow",
                "Conversation/Change Operation")).document();
        String documentBlueId = storedDocumentBlueId(initialized);
        Node event = operationRequestEvent(blue, "increment", 6, false, documentBlueId, "owner-42");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("6"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationWithoutDeclaredOrDerivedChannelFailsInitialization() {
        Blue blue = new Blue();
        Node document = operationWorkflowDocument(null, null);

        assertThrows(IllegalStateException.class, () -> blue.initializeDocument(document));
    }

    @Test
    void sequentialWorkflowOperationAppliesHandlerEventPattern() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocumentAdvanced(
                null,
                "ownerChannel",
                "Conversation/Sequential Workflow Operation",
                "Conversation/Operation",
                "type: Integer",
                "message:\n  allowNewerVersion: false",
                "${event.message.request + document('/counter')}")).document();

        String storedBlueId = storedDocumentBlueId(initialized);
        Node matchingEvent = operationRequestEvent(blue, "increment", 2, false, storedBlueId, "owner-42");
        DocumentProcessingResult matched = blue.processDocument(initialized, matchingEvent);
        assertEquals(new BigInteger("2"), matched.document().getProperties().get("counter").getValue());

        Node nonMatchingEvent = operationRequestEvent(blue, "increment", 5, true, storedBlueId, "owner-42");
        DocumentProcessingResult skipped = blue.processDocument(matched.document(), nonMatchingEvent);
        assertEquals(new BigInteger("2"), skipped.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationHandlesComplexRequestStructures() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocumentAdvanced(
                null,
                "ownerChannel",
                "Conversation/Sequential Workflow Operation",
                "Conversation/Operation",
                "type: Dictionary\nentries:\n  amount:\n    type: Integer\n  metadata:\n    type: Dictionary\n    entries:\n      note:\n        type: Text",
                null,
                "${event.message.request.amount + document('/counter')}")).document();

        String storedBlueId = storedDocumentBlueId(initialized);
        Node complexEvent = blue.yamlToNode("type:\n" +
                "  blueId: Conversation/Timeline Entry\n" +
                "eventId: evt-op-complex\n" +
                "timeline:\n" +
                "  timelineId: owner-42\n" +
                "message:\n" +
                "  type:\n" +
                "    blueId: Conversation/Operation Request\n" +
                "  operation: increment\n" +
                "  allowNewerVersion: false\n" +
                "  document:\n" +
                "    blueId: " + storedBlueId + "\n" +
                "  request:\n" +
                "    amount: 3\n" +
                "    metadata:\n" +
                "      note: boost\n");

        DocumentProcessingResult result = blue.processDocument(initialized, complexEvent);

        assertEquals(new BigInteger("3"), result.document().getProperties().get("counter").getValue());
    }

    @Test
    void sequentialWorkflowOperationExecutesDerivedChangeWorkflow() {
        Blue blue = new Blue();
        Node initialized = blue.initializeDocument(operationWorkflowDocumentAdvanced(
                null,
                "ownerChannel",
                "Conversation/Change Workflow",
                "Conversation/Change Operation",
                "type: Conversation/Change Request",
                null,
                "${event.message.request.changeset[0].val}")).document();

        String storedBlueId = storedDocumentBlueId(initialized);
        Node changeEvent = blue.yamlToNode("type:\n" +
                "  blueId: Conversation/Timeline Entry\n" +
                "eventId: evt-op-change\n" +
                "timeline:\n" +
                "  timelineId: owner-42\n" +
                "message:\n" +
                "  type:\n" +
                "    blueId: Conversation/Operation Request\n" +
                "  operation: increment\n" +
                "  allowNewerVersion: false\n" +
                "  document:\n" +
                "    blueId: " + storedBlueId + "\n" +
                "  request:\n" +
                "    type:\n" +
                "      blueId: Conversation/Change Request\n" +
                "    changeset:\n" +
                "      - op: REPLACE\n" +
                "        path: /counter\n" +
                "        val: 11\n");

        DocumentProcessingResult result = blue.processDocument(initialized, changeEvent);

        assertEquals(new BigInteger("11"), result.document().getProperties().get("counter").getValue());
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
                "        code: \"({ changeset: [{ op: 'ADD', path: '/jsValue', val: 9 }], events: [{ kind: 'js' }] })\"\n" +
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
    void javaScriptCodeStepHasCurrentContractBindings() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: JavaScript Contract Binding Doc\n" +
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
                "          blueId: Conversation/JavaScript Code\n" +
                "        code: \"({ changeset: [{ op: 'ADD', path: '/contractChannel', val: currentContract.channel }] })\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-bind").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals("testChannel", result.document().getProperties().get("contractChannel").getValue());
    }

    @Test
    void javaScriptCodeStepErrorsBecomeFatalTermination() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: JavaScript Error Doc\n" +
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
                "          blueId: Conversation/JavaScript Code\n" +
                "        code: \"throw new Error('boom')\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-err").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        Node terminated = result.document()
                .getProperties().get("contracts")
                .getProperties().get("terminated");
        assertNotNull(terminated);
        assertEquals("fatal", String.valueOf(terminated.getProperties().get("cause").getValue()));
        assertTrue(String.valueOf(terminated.getProperties().get("reason").getValue()).contains("Failed to evaluate code block"));
    }

    @Test
    void javaScriptCodeStepSupportsEmitCallbackStyle() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: JavaScript Emit Callback Doc\n" +
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
                "        code: \"emit({ kind: 'js-callback' }); ({ changeset: [{ op: 'ADD', path: '/callbackHit', val: 11 }] })\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-callback").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("11"), result.document().getProperties().get("callbackHit").getValue());
        assertTrue(result.triggeredEvents().stream()
                .anyMatch(emitted -> emitted != null
                        && emitted.getProperties() != null
                        && emitted.getProperties().get("kind") != null
                        && "js-callback".equals(String.valueOf(emitted.getProperties().get("kind").getValue()))));
    }

    @Test
    void javaScriptCodeStepCanonicalDocumentReadIncludesTypeMetadata() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());

        Node document = blue.yamlToNode("name: JavaScript Canonical Read Doc\n" +
                "base: 5\n" +
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
                "          blueId: Conversation/JavaScript Code\n" +
                "        code: \"({ changeset: [{ op: 'ADD', path: '/canonicalType', val: document.canonical('/base').type.blueId }] })\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-canon").kind("TestEvent"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(INTEGER_TYPE_BLUE_ID, result.document().getProperties().get("canonicalType").getValue());
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
                "            val: \"${event.count + document('/base')}\"\n");

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
                "          kind: \"${event.kind}\"\n" +
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
        return operationWorkflowDocument(
                handlerChannel,
                operationChannel,
                "Conversation/Sequential Workflow Operation",
                "Conversation/Operation");
    }

    private Node operationWorkflowDocument(String handlerChannel,
                                           String operationChannel,
                                           String handlerTypeBlueId,
                                           String operationTypeBlueId) {
        return operationWorkflowDocumentAdvanced(
                handlerChannel,
                operationChannel,
                handlerTypeBlueId,
                operationTypeBlueId,
                "type: Integer",
                null,
                "${event.message.request}");
    }

    private Node operationWorkflowDocumentAdvanced(String handlerChannel,
                                                   String operationChannel,
                                                   String handlerTypeBlueId,
                                                   String operationTypeBlueId,
                                                   String requestTypeYaml,
                                                   String handlerEventYaml,
                                                   String stepExpression) {
        String handlerChannelYaml = handlerChannel != null
                ? "    channel: " + handlerChannel + "\n"
                : "";
        String operationChannelYaml = operationChannel != null
                ? "    channel: " + operationChannel + "\n"
                : "";
        String handlerEventSection = handlerEventYaml != null && !handlerEventYaml.trim().isEmpty()
                ? "    event:\n" + indent(handlerEventYaml, 6) + "\n"
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
                "      blueId: " + operationTypeBlueId + "\n" +
                operationChannelYaml +
                "    request:\n" +
                indent(requestTypeYaml, 6) + "\n" +
                "  operationWorkflow:\n" +
                "    type:\n" +
                "      blueId: " + handlerTypeBlueId + "\n" +
                handlerChannelYaml +
                "    operation: increment\n" +
                handlerEventSection +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /counter\n" +
                "            val: \"" + stepExpression + "\"\n");
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

    private String indent(String block, int spaces) {
        StringBuilder builder = new StringBuilder();
        String prefix = new String(new char[spaces]).replace('\0', ' ');
        String[] lines = block.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(prefix).append(lines[i]);
        }
        return builder.toString();
    }
}
