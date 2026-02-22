package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TriggerEventStepExecutor implements WorkflowStepExecutor {

    @Override
    public Set<String> supportedBlueIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(java.util.Arrays.asList(
                "Conversation/Trigger Event",
                "TriggerEvent"
        )));
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        Node stepNode = args.stepNode();
        if (stepNode == null || stepNode.getProperties() == null) {
            args.context().throwFatal("Trigger Event step payload is invalid");
            return null;
        }
        Node eventNode = stepNode.getProperties().get("event");
        if (eventNode == null) {
            args.context().throwFatal("Trigger Event step must declare event payload");
            return null;
        }
        args.context().chargeTriggerEventBase();
        args.context().emitEvent(eventNode.clone());
        return null;
    }
}
