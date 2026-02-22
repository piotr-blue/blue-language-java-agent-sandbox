package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId({"Conversation/Operation", "Operation"})
public class OperationMarker extends MarkerContract {

    private String channel;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
