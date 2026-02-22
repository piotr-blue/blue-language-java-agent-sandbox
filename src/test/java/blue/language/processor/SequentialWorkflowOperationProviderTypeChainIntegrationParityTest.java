package blue.language.processor;

import blue.language.Blue;
import blue.language.NodeProvider;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequentialWorkflowOperationProviderTypeChainIntegrationParityTest {

    @Test
    void operationRequestTypeMatchingSupportsProviderBackedTypeChains() {
        NodeProvider provider = new NodeProvider() {
            @Override
            public List<Node> fetchByBlueId(String blueId) {
                if (!"Custom/Derived Request".equals(blueId)) {
                    return Collections.emptyList();
                }
                Node definition = new Node();
                definition.type(new Node().blueId("Custom/Base Request"));
                return Collections.singletonList(definition);
            }
        };

        Blue blue = new Blue(provider);
        Node document = blue.yamlToNode("name: Provider Request Type Chain Operation Doc\n" +
                "counter: 0\n" +
                "contracts:\n" +
                "  ownerChannel:\n" +
                "    type:\n" +
                "      blueId: Conversation/Timeline Channel\n" +
                "    timelineId: owner-42\n" +
                "  increment:\n" +
                "    type:\n" +
                "      blueId: Conversation/Operation\n" +
                "    channel: ownerChannel\n" +
                "    request:\n" +
                "      type:\n" +
                "        blueId: Custom/Base Request\n" +
                "  operationWorkflow:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow Operation\n" +
                "    operation: increment\n" +
                "    steps:\n" +
                "      - type:\n" +
                "          blueId: Conversation/Update Document\n" +
                "        changeset:\n" +
                "          - op: REPLACE\n" +
                "            path: /counter\n" +
                "            val: \"${event.message.request.payload}\"\n");

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.yamlToNode("type:\n" +
                "  blueId: Conversation/Timeline Entry\n" +
                "eventId: evt-provider-request-type\n" +
                "timeline:\n" +
                "  timelineId: owner-42\n" +
                "message:\n" +
                "  type:\n" +
                "    blueId: Conversation/Operation Request\n" +
                "  operation: increment\n" +
                "  request:\n" +
                "    type:\n" +
                "      blueId: Custom/Derived Request\n" +
                "    payload: 12\n");

        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("12"), result.document().getProperties().get("counter").getValue());
    }
}
