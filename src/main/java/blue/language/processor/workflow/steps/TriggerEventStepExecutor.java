package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.script.QuickJSEvaluator;
import blue.language.processor.script.QuickJsExpressionUtils;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TriggerEventStepExecutor implements WorkflowStepExecutor {

    private final QuickJSEvaluator evaluator;

    public TriggerEventStepExecutor() {
        this(new QuickJSEvaluator());
    }

    public TriggerEventStepExecutor(QuickJSEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Set<String> supportedBlueIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(java.util.Arrays.asList(
                "Conversation/Trigger Event",
                "TriggerEvent"
        )));
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        Node stepNode = QuickJsExpressionUtils.resolveExpressions(
                args.stepNode(),
                evaluator,
                QuickJSStepBindings.create(args),
                args.context(),
                new QuickJsExpressionUtils.PointerPredicate() {
                    @Override
                    public boolean test(String pointer, Node node) {
                        return "/event".equals(pointer) || pointer.startsWith("/event/");
                    }
                },
                new QuickJsExpressionUtils.PointerPredicate() {
                    @Override
                    public boolean test(String pointer, Node node) {
                        if (!pointer.startsWith("/event/")) {
                            return true;
                        }
                        return !isEmbeddedDocumentNode(node);
                    }
                });
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

    private boolean isEmbeddedDocumentNode(Node node) {
        return node != null
                && node.getProperties() != null
                && node.getProperties().containsKey("contracts");
    }
}
