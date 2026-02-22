package blue.language.processor.model;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TypeBlueId({"Conversation/Sequential Workflow Operation", "SequentialWorkflowOperation"})
public class SequentialWorkflowOperation extends HandlerContract {

    private String operation;
    private final List<Node> steps = new ArrayList<>();

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<Node> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public void setSteps(List<Node> steps) {
        this.steps.clear();
        if (steps == null) {
            return;
        }
        for (Node step : steps) {
            this.steps.add(step != null ? step.clone() : null);
        }
    }
}
