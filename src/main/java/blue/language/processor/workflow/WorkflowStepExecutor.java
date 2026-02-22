package blue.language.processor.workflow;

import java.util.Set;

public interface WorkflowStepExecutor {

    Set<String> supportedBlueIds();

    Object execute(StepExecutionArgs args);
}
