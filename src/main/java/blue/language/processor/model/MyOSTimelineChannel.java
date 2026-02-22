package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId({"MyOS/MyOS Timeline Channel", "MyOSTimelineChannel"})
public class MyOSTimelineChannel extends ChannelContract {

    private String timelineId;

    public String getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(String timelineId) {
        this.timelineId = timelineId;
    }
}
