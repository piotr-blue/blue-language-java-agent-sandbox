package blue.language.processor.model;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

@TypeBlueId("Conversation/Sequential Workflow Operation")
public class ConversationSequentialWorkflowOperation extends Contract {

    private String operation;
    private Node steps;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Node getSteps() {
        return steps;
    }

    public void setSteps(Node steps) {
        this.steps = steps;
    }
}
