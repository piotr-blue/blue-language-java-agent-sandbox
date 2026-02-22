package blue.language.processor.model;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

@TypeBlueId({"Conversation/Timeline Channel", "TimelineChannel"})
public class TimelineChannel extends ChannelContract {

    private String timelineId;
    private Node event;

    public String getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(String timelineId) {
        this.timelineId = timelineId;
    }

    public Node getEvent() {
        return event;
    }

    public void setEvent(Node event) {
        this.event = event;
    }
}
