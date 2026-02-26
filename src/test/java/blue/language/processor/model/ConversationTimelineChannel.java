package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId("Conversation/Timeline Channel")
public class ConversationTimelineChannel extends ChannelContract {

    private String timelineId;

    public String getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(String timelineId) {
        this.timelineId = timelineId;
    }
}
