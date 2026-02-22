package blue.language.processor.workflow;

import blue.language.model.Node;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.workflow.steps.JavaScriptCodeStepExecutor;
import blue.language.processor.workflow.steps.TriggerEventStepExecutor;
import blue.language.processor.workflow.steps.UpdateDocumentStepExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowStepRunner {

    private final Map<String, WorkflowStepExecutor> executorsByBlueId = new LinkedHashMap<>();

    public WorkflowStepRunner(List<WorkflowStepExecutor> executors) {
        if (executors != null) {
            for (WorkflowStepExecutor executor : executors) {
                if (executor == null || executor.supportedBlueIds() == null) {
                    continue;
                }
                for (String blueId : executor.supportedBlueIds()) {
                    if (blueId != null && !blueId.trim().isEmpty()) {
                        executorsByBlueId.put(blueId, executor);
                    }
                }
            }
        }
    }

    public static WorkflowStepRunner defaultRunner() {
        List<WorkflowStepExecutor> defaults = new ArrayList<>();
        defaults.add(new TriggerEventStepExecutor());
        defaults.add(new JavaScriptCodeStepExecutor());
        defaults.add(new UpdateDocumentStepExecutor());
        return new WorkflowStepRunner(defaults);
    }

    public Map<String, Object> run(HandlerContract workflow, List<Node> steps, Node eventNode, ProcessorExecutionContext context) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> results = new LinkedHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            Node stepNode = steps.get(i);
            if (stepNode == null || stepNode.getType() == null || stepNode.getType().getBlueId() == null) {
                context.throwFatal("Sequential workflow step is missing type metadata");
                return results;
            }
            String stepBlueId = stepNode.getType().getBlueId();
            WorkflowStepExecutor executor = executorsByBlueId.get(stepBlueId);
            if (executor == null) {
                context.throwFatal("Unsupported workflow step type \"" + stepBlueId + "\"");
                return results;
            }
            Object value = executor.execute(new StepExecutionArgs(
                    workflow,
                    stepNode,
                    eventNode,
                    context,
                    results,
                    i));
            if (value != null) {
                String key = resolveResultKey(stepNode, i);
                results.put(key, value);
            }
        }
        return results;
    }

    private String resolveResultKey(Node stepNode, int index) {
        if (stepNode != null && stepNode.getName() != null && !stepNode.getName().trim().isEmpty()) {
            return stepNode.getName().trim();
        }
        return "Step" + (index + 1);
    }
}
