package blue.language.processor.model.core;

import blue.language.model.BlueType;
import blue.language.model.Node;

@BlueType("Core.Handler")
public class CoreHandlerType extends CoreContractType {

    private String channel;
    private Node event;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Node getEvent() {
        return event;
    }

    public void setEvent(Node event) {
        this.event = event;
    }
}
