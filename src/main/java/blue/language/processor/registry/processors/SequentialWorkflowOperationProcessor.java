package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.ContractBundle;
import blue.language.processor.HandlerProcessor;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.MarkerContract;
import blue.language.processor.model.OperationMarker;
import blue.language.processor.model.SequentialWorkflowOperation;
import blue.language.processor.workflow.WorkflowStepRunner;

import java.util.List;

public class SequentialWorkflowOperationProcessor implements HandlerProcessor<SequentialWorkflowOperation> {

    private final WorkflowStepRunner stepRunner;

    public SequentialWorkflowOperationProcessor() {
        this(WorkflowStepRunner.defaultRunner());
    }

    public SequentialWorkflowOperationProcessor(WorkflowStepRunner stepRunner) {
        this.stepRunner = stepRunner;
    }

    @Override
    public Class<SequentialWorkflowOperation> contractType() {
        return SequentialWorkflowOperation.class;
    }

    @Override
    public String deriveChannel(SequentialWorkflowOperation contract) {
        if (contract.getChannelKey() != null && !contract.getChannelKey().trim().isEmpty()) {
            return contract.getChannelKey().trim();
        }
        return null;
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
        stepRunner.run(contract, steps, context.event(), context);
    }

    @Override
    public String deriveChannel(SequentialWorkflowOperation contract, ContractBundle scopeContracts) {
        String declared = deriveChannel(contract);
        if (declared != null) {
            return declared;
        }
        if (scopeContracts == null || contract.getOperation() == null || contract.getOperation().trim().isEmpty()) {
            return null;
        }
        String operationKey = contract.getOperation().trim();
        MarkerContract marker = scopeContracts.marker(operationKey);
        if (!(marker instanceof OperationMarker)) {
            return null;
        }
        String channel = ((OperationMarker) marker).getChannel();
        return channel != null && !channel.trim().isEmpty() ? channel.trim() : null;
    }
}
