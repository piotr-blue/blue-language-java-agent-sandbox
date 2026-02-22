package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.HandlerProcessor;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.SequentialWorkflowOperation;

import java.util.List;

public class SequentialWorkflowOperationProcessor implements HandlerProcessor<SequentialWorkflowOperation> {

    @Override
    public Class<SequentialWorkflowOperation> contractType() {
        return SequentialWorkflowOperation.class;
    }

    @Override
    public boolean matches(SequentialWorkflowOperation contract, ProcessorExecutionContext context) {
        return WorkflowContractSupport.matchesEventFilter(context.event(), contract.getEvent());
    }

    @Override
    public void execute(SequentialWorkflowOperation contract, ProcessorExecutionContext context) {
        List<Node> steps = contract.getSteps();
        if (steps == null || steps.isEmpty()) {
            return;
        }
        // Step execution for sequential workflow operations is wired in phase 5.
    }
}
