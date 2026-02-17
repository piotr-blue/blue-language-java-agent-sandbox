package blue.language.processor.model.core;

import blue.language.model.BlueType;
import blue.language.model.Node;

@BlueType("Core.Channel")
public class CoreChannelType extends CoreContractType {

    private Node event;

    public Node getEvent() {
        return event;
    }

    public void setEvent(Node event) {
        this.event = event;
    }
}
