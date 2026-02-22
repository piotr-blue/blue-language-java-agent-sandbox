package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.ChannelEvaluationContext;
import blue.language.processor.ChannelProcessorEvaluation;
import blue.language.processor.ContractBundle;
import blue.language.processor.ContractProcessorRegistry;
import blue.language.processor.model.ChannelEventCheckpoint;
import blue.language.processor.model.CompositeTimelineChannel;
import blue.language.processor.model.MyOSTimelineChannel;
import blue.language.processor.model.TimelineChannel;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeTimelineChannelProcessorTest {

    @Test
    void evaluateBuildsDeliveriesWithCompositeCheckpointKeys() {
        CompositeTimelineChannel composite = new CompositeTimelineChannel();
        composite.setChannels(Collections.singletonList("childA"));

        TimelineChannel child = new TimelineChannel();
        child.setTimelineId("alpha");

        ContractBundle bundle = ContractBundle.builder()
                .addChannel("childA", child)
                .addChannel("composite", composite)
                .build();

        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        registry.registerChannel(new TimelineChannelProcessor());
        registry.registerChannel(new CompositeTimelineChannelProcessor());

        Node event = timelineEvent("alpha", 10L);
        ChannelEvaluationContext context = new ChannelEvaluationContext(
                "/",
                "composite",
                event,
                null,
                bundle.markers(),
                bundle,
                registry);

        CompositeTimelineChannelProcessor processor = new CompositeTimelineChannelProcessor();
        ChannelProcessorEvaluation evaluation = processor.evaluate(composite, context);

        assertTrue(evaluation.matches());
        assertEquals(1, evaluation.deliveries().size());
        ChannelProcessorEvaluation.ChannelDelivery delivery = evaluation.deliveries().get(0);
        assertEquals("composite::childA", delivery.checkpointKey());
        assertEquals("10", delivery.eventId());
        assertNotNull(delivery.eventNode().getProperties().get("meta").getProperties().get("compositeSourceChannelKey"));
    }

    @Test
    void evaluateMarksDeliveryAsStaleWhenCheckpointHasNewerChildEvent() {
        CompositeTimelineChannel composite = new CompositeTimelineChannel();
        composite.setChannels(Collections.singletonList("child"));

        MyOSTimelineChannel child = new MyOSTimelineChannel();
        child.setTimelineId("beta");

        ChannelEventCheckpoint checkpoint = new ChannelEventCheckpoint();
        checkpoint.putEvent("composite::child", timelineEvent("beta", 50L));
        checkpoint.updateSignature("composite::child", "sig");

        ContractBundle bundle = ContractBundle.builder()
                .addChannel("child", child)
                .addChannel("composite", composite)
                .addMarker("checkpoint", checkpoint)
                .build();

        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        registry.registerChannel(new MyOSTimelineChannelProcessor());
        registry.registerChannel(new CompositeTimelineChannelProcessor());

        ChannelEvaluationContext context = new ChannelEvaluationContext(
                "/",
                "composite",
                timelineEvent("beta", 5L),
                null,
                bundle.markers(),
                bundle,
                registry);

        CompositeTimelineChannelProcessor processor = new CompositeTimelineChannelProcessor();
        ChannelProcessorEvaluation evaluation = processor.evaluate(composite, context);

        assertTrue(evaluation.matches());
        assertEquals(1, evaluation.deliveries().size());
        assertFalse(Boolean.TRUE.equals(evaluation.deliveries().get(0).shouldProcess()));
    }

    private static Node timelineEvent(String timelineId, long sequence) {
        Node timeline = new Node().properties("timelineId", new Node().value(timelineId));
        return new Node()
                .properties("eventId", new Node().value(String.valueOf(sequence)))
                .properties("timeline", timeline)
                .properties("sequence", new Node().value(sequence));
    }
}
