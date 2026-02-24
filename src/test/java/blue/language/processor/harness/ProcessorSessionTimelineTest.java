package blue.language.processor.harness;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorSessionTimelineTest {

    @Test
    void callOperationBuildsTimelineEnvelopeAndProcessesWorkflowOperation() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Harness Operation Doc\n"
                + "counter: 0\n"
                + "contracts:\n"
                + "  ownerChannel:\n"
                + "    type:\n"
                + "      blueId: Conversation/Timeline Channel\n"
                + "    timelineId: owner-42\n"
                + "  increment:\n"
                + "    type:\n"
                + "      blueId: Conversation/Operation\n"
                + "    channel: ownerChannel\n"
                + "    request:\n"
                + "      type: Integer\n"
                + "  incrementImpl:\n"
                + "    type:\n"
                + "      blueId: Conversation/Sequential Workflow Operation\n"
                + "    operation: increment\n"
                + "    steps:\n"
                + "      - type:\n"
                + "          blueId: Conversation/Update Document\n"
                + "        changeset:\n"
                + "          - op: REPLACE\n"
                + "            path: /counter\n"
                + "            val: \"${event.message.request}\"\n");

        ProcessorSession session = new ProcessorHarness(blue.getDocumentProcessor())
                .start(document)
                .initSession()
                .registerParticipant("owner", "owner-42");

        session.callOperation("owner", "increment", Integer.valueOf(8));
        int processed = session.runUntilIdle();

        assertEquals(1, processed);
        assertEquals(new BigInteger("8"), session.document().getProperties().get("counter").getValue());

        List<Node> ownerTimelineEntries = session.timelineStore().entries("owner-42");
        assertEquals(1, ownerTimelineEntries.size());
        Node recorded = ownerTimelineEntries.get(0);
        assertEquals("Conversation/Timeline Entry", recorded.getType().getBlueId());
        assertEquals("Conversation/Operation Request",
                recorded.getProperties().get("message").getType().getBlueId());
        assertEquals("increment",
                String.valueOf(recorded.getProperties().get("message").getProperties().get("operation").getValue()));
    }

    @Test
    void eventFactoryProducesCanonicalTimelineEntryShapes() {
        Node requestEntry = EventFactory.conversationOperationRequestEntry(
                "owner-42",
                "evt-1",
                "increment",
                new Node().value(3));
        assertEquals("Conversation/Timeline Entry", requestEntry.getType().getBlueId());
        assertEquals("owner-42",
                String.valueOf(requestEntry.getProperties().get("timeline").getProperties().get("timelineId").getValue()));
        assertEquals("Conversation/Operation Request",
                requestEntry.getProperties().get("message").getType().getBlueId());
        assertEquals("increment",
                String.valueOf(requestEntry.getProperties().get("message").getProperties().get("operation").getValue()));

        Node myOsEntry = EventFactory.myOsTimelineEntry(
                "owner-42",
                "evt-2",
                new Node().type(new Node().blueId("Conversation/Chat Message")));
        assertEquals("MyOS/MyOS Timeline Entry", myOsEntry.getType().getBlueId());
        assertTrue("owner-42".equals(EventFactory.timelineId(myOsEntry)));
    }
}
