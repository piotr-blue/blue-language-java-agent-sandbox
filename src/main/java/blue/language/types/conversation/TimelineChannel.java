package blue.language.types.conversation;

import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;
import blue.language.types.core.Channel;

@TypeAlias("Conversation/Timeline Channel")
@TypeBlueId("EvuCWsG1E6WJQg8QXmk6rwMANYTZjoLWVZ1vYQWUwdTH")
public class TimelineChannel extends Channel {
    public String timelineId;

    public TimelineChannel timelineId(String timelineId) {
        this.timelineId = timelineId;
        return this;
    }
}
