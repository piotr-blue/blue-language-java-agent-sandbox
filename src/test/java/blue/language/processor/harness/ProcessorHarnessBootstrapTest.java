package blue.language.processor.harness;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorHarnessBootstrapTest {

    @Test
    void startOnRawDocumentAssignsTimelineIdsAndRegistersChannelAliases() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Harness Raw Mapping Doc\n"
                + "counter: 0\n"
                + "contracts:\n"
                + "  payerChannel:\n"
                + "    type:\n"
                + "      blueId: Conversation/Timeline Channel\n"
                + "  confirm:\n"
                + "    type:\n"
                + "      blueId: Conversation/Operation\n"
                + "    channel: payerChannel\n"
                + "    request:\n"
                + "      type: Integer\n"
                + "  confirmImpl:\n"
                + "    type:\n"
                + "      blueId: Conversation/Sequential Workflow Operation\n"
                + "    operation: confirm\n"
                + "    steps:\n"
                + "      - type:\n"
                + "          blueId: Conversation/Update Document\n"
                + "        changeset:\n"
                + "          - op: REPLACE\n"
                + "            path: /counter\n"
                + "            val: \"${event.message.request}\"\n");

        ProcessorSession session = new ProcessorHarness(blue.getDocumentProcessor()).start(document).initSession();
        session.callOperation("payer", "confirm", Integer.valueOf(5));
        session.runUntilIdle();

        assertEquals("payerChannel-timeline",
                String.valueOf(session.document()
                        .getProperties().get("contracts")
                        .getProperties().get("payerChannel")
                        .getProperties().get("timelineId").getValue()));
        assertEquals(new BigInteger("5"), session.document().getProperties().get("counter").getValue());
        assertTrue(session.participants().containsKey("payerChannel"));
        assertTrue(session.participants().containsKey("payer"));
    }

    @Test
    void startOnBootstrapUsesBindingIdentityAsTimelineId() {
        Blue blue = new Blue();
        Node bootstrap = blue.yamlToNode("type:\n"
                + "  blueId: MyOS/Document Session Bootstrap\n"
                + "document:\n"
                + "  name: Harness Bootstrap Doc\n"
                + "  counter: 0\n"
                + "  contracts:\n"
                + "    payerChannel:\n"
                + "      type:\n"
                + "        blueId: Conversation/Timeline Channel\n"
                + "    confirm:\n"
                + "      type:\n"
                + "        blueId: Conversation/Operation\n"
                + "      channel: payerChannel\n"
                + "      request:\n"
                + "        type: Integer\n"
                + "    confirmImpl:\n"
                + "      type:\n"
                + "        blueId: Conversation/Sequential Workflow Operation\n"
                + "      operation: confirm\n"
                + "      steps:\n"
                + "        - type:\n"
                + "            blueId: Conversation/Update Document\n"
                + "          changeset:\n"
                + "            - op: REPLACE\n"
                + "              path: /counter\n"
                + "              val: \"${event.message.request}\"\n"
                + "channelBindings:\n"
                + "  payerChannel:\n"
                + "    email: alice@example.com\n");

        ProcessorSession session = new ProcessorHarness(blue.getDocumentProcessor()).start(bootstrap).initSession();
        session.callOperation("payerChannel", "confirm", Integer.valueOf(11));
        session.runUntilIdle();

        assertEquals(new BigInteger("11"), session.document().getProperties().get("counter").getValue());
        assertEquals("alice@example.com",
                String.valueOf(session.document()
                        .getProperties().get("contracts")
                        .getProperties().get("payerChannel")
                        .getProperties().get("timelineId").getValue()));
        List<Node> timelineEntries = session.timelineStore().entries("alice@example.com");
        assertEquals(1, timelineEntries.size());
    }
}
