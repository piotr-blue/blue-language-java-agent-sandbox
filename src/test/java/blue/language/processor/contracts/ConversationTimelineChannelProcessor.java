package blue.language.processor.contracts;

import blue.language.model.Node;
import blue.language.processor.ChannelEvaluationContext;
import blue.language.processor.ChannelProcessor;
import blue.language.processor.model.ConversationTimelineChannel;

public class ConversationTimelineChannelProcessor implements ChannelProcessor<ConversationTimelineChannel> {

    @Override
    public Class<ConversationTimelineChannel> contractType() {
        return ConversationTimelineChannel.class;
    }

    @Override
    public boolean matches(ConversationTimelineChannel contract, ChannelEvaluationContext context) {
        Node event = context.event();
        if (event == null || contract.getKey() == null) {
            return false;
        }
        String eventChannel = event.getAsText("/channel/value");
        if (eventChannel == null) {
            eventChannel = event.getAsText("/channel");
        }
        if (eventChannel == null) {
            return false;
        }
        return contract.getKey().equals(eventChannel.trim());
    }
}
