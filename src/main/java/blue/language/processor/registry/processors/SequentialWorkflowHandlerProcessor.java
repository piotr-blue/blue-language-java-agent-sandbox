package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.HandlerProcessor;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.SequentialWorkflow;

import java.util.List;

public class SequentialWorkflowHandlerProcessor implements HandlerProcessor<SequentialWorkflow> {

    @Override
    public Class<SequentialWorkflow> contractType() {
        return SequentialWorkflow.class;
    }

    @Override
    public boolean matches(SequentialWorkflow contract, ProcessorExecutionContext context) {
        return WorkflowContractSupport.matchesEventFilter(context.event(), contract.getEvent());
    }

    @Override
    public void execute(SequentialWorkflow contract, ProcessorExecutionContext context) {
        List<Node> steps = contract.getSteps();
        if (steps == null || steps.isEmpty()) {
            return;
        }
        // Step execution is handled by dedicated runtime executors; this processor
        // currently validates matching and wiring parity.
    }
}
