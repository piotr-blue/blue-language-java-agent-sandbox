package blue.language.processor.harness;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorSessionTest {

    @Test
    void initRunsLifecycleAndCapturesEmissions() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Harness Init Doc\n"
                + "contracts:\n"
                + "  lifecycleChannel:\n"
                + "    type:\n"
                + "      blueId: Core/Lifecycle Event Channel\n"
                + "  onLifecycle:\n"
                + "    channel: lifecycleChannel\n"
                + "    type:\n"
                + "      blueId: Conversation/Sequential Workflow\n"
                + "    event:\n"
                + "      type:\n"
                + "        blueId: Core/Document Processing Initiated\n"
                + "    steps:\n"
                + "      - type:\n"
                + "          blueId: Conversation/Update Document\n"
                + "        changeset:\n"
                + "          - op: REPLACE\n"
                + "            path: /initializedByHarness\n"
                + "            val: 1\n");

        ProcessorSession session = new ProcessorHarness(blue.getDocumentProcessor()).start(document);
        DocumentProcessingResult result = session.init();

        assertFalse(result.capabilityFailure());
        assertTrue(session.initialized());
        assertEquals(1, session.emittedEvents().size());
        assertEquals("Core/Document Processing Initiated",
                String.valueOf(session.emittedEvents().get(0).getProperties().get("type").getValue()));
        assertEquals(new BigInteger("1"), session.document().getProperties().get("initializedByHarness").getValue());
    }

    @Test
    void runUntilIdleProcessesQueuedExternalEvents() {
        Blue blue = new Blue();
        Node document = blue.yamlToNode("name: Harness External Event Doc\n"
                + "contracts:\n"
                + "  timelineChannel:\n"
                + "    type:\n"
                + "      blueId: Conversation/Timeline Channel\n"
                + "    timelineId: alice\n"
                + "  onTimeline:\n"
                + "    channel: timelineChannel\n"
                + "    type:\n"
                + "      blueId: Conversation/Sequential Workflow\n"
                + "    steps:\n"
                + "      - type:\n"
                + "          blueId: Conversation/Update Document\n"
                + "        changeset:\n"
                + "          - op: REPLACE\n"
                + "            path: /counter\n"
                + "            val: 7\n");

        ProcessorSession session = new ProcessorHarness(blue.getDocumentProcessor()).start(document);
        session.init();
        session.pushEvent(blue.yamlToNode("type:\n"
                + "  blueId: Conversation/Timeline Entry\n"
                + "eventId: evt-1\n"
                + "timeline:\n"
                + "  timelineId: alice\n"
                + "message:\n"
                + "  type:\n"
                + "    blueId: Conversation/Chat Message\n"
                + "  message: hello\n"));

        int processed = session.runUntilIdle();

        assertEquals(1, processed);
        assertEquals(0, session.pendingInboundEvents());
        assertEquals(new BigInteger("7"), session.document().getProperties().get("counter").getValue());
    }
}
