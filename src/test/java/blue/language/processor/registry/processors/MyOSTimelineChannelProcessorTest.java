package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.ChannelEvaluationContext;
import blue.language.processor.model.MyOSTimelineChannel;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyOSTimelineChannelProcessorTest {

    @Test
    void matchesTimelineEventsAndUsesRecencyComparison() {
        MyOSTimelineChannelProcessor processor = new MyOSTimelineChannelProcessor();
        MyOSTimelineChannel contract = new MyOSTimelineChannel();
        contract.setTimelineId("owner-42");

        Node current = new Node()
                .properties("eventId", new Node().value("evt-current"))
                .properties("timeline", new Node().properties("timelineId", new Node().value("owner-42")))
                .properties("revision", new Node().value(new BigInteger("9")));
        ChannelEvaluationContext context = new ChannelEvaluationContext("/", "myos", current, null, null, null, null);

        assertTrue(processor.matches(contract, context));
        assertEquals("evt-current", processor.eventId(contract, context));
        assertTrue(processor.isNewerEvent(contract, context,
                new Node().properties("timeline", new Node().properties("timelineId", new Node().value("owner-42")))
                        .properties("revision", new Node().value(new BigInteger("8")))));
        assertFalse(processor.isNewerEvent(contract, context,
                new Node().properties("timeline", new Node().properties("timelineId", new Node().value("owner-42")))
                        .properties("revision", new Node().value(new BigInteger("12")))));
    }
}
