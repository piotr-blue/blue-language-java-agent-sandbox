package blue.language.processor.model;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

@TypeBlueId("Conversation/Operation")
public class ConversationOperation extends HandlerContract {

    private String description;
    private Node request;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Node getRequest() {
        return request;
    }

    public void setRequest(Node request) {
        this.request = request;
    }
}
