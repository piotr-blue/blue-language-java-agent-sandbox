package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.ChannelEvaluationContext;
import blue.language.processor.ChannelProcessor;
import blue.language.processor.model.TimelineChannel;

public class TimelineChannelProcessor implements ChannelProcessor<TimelineChannel> {

    @Override
    public Class<TimelineChannel> contractType() {
        return TimelineChannel.class;
    }

    @Override
    public boolean matches(TimelineChannel contract, ChannelEvaluationContext context) {
        String expectedTimelineId = contract.getTimelineId();
        if (expectedTimelineId == null || expectedTimelineId.trim().isEmpty()) {
            return false;
        }
        String eventTimelineId = TimelineEventSupport.timelineId(context.event());
        return expectedTimelineId.trim().equals(eventTimelineId);
    }

    @Override
    public Node channelize(TimelineChannel contract, ChannelEvaluationContext context) {
        Node event = context.event();
        return event != null ? event.clone() : null;
    }

    @Override
    public String eventId(TimelineChannel contract, ChannelEvaluationContext context) {
        return TimelineEventSupport.eventId(context.event());
    }

    @Override
    public boolean isNewerEvent(TimelineChannel contract, ChannelEvaluationContext context, Node lastEvent) {
        return TimelineEventSupport.isNewer(context.event(), lastEvent);
    }
}
