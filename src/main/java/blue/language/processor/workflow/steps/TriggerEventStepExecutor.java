package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.script.QuickJSEvaluator;
import blue.language.processor.script.QuickJsExpressionUtils;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TriggerEventStepExecutor implements WorkflowStepExecutor {

    private static final Set<String> SUPPORTED_BLUE_IDS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(java.util.Arrays.asList(
                    "Conversation/Trigger Event",
                    "TriggerEvent"
            )));

    private final QuickJSEvaluator evaluator;

    public TriggerEventStepExecutor() {
        this(new QuickJSEvaluator());
    }

    public TriggerEventStepExecutor(QuickJSEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Set<String> supportedBlueIds() {
        return SUPPORTED_BLUE_IDS;
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        if (!isValidStepNode(args.stepNode())) {
            args.context().throwFatal("Trigger Event step payload is invalid");
            return null;
        }
        Node stepNode = QuickJsExpressionUtils.resolveExpressions(
                args.stepNode(),
                evaluator,
                QuickJSStepBindings.create(args),
                args.context(),
                QuickJsExpressionUtils.createPathPredicate(
                        java.util.Arrays.asList("/event", "/event/**"),
                        null),
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

    private boolean isValidStepNode(Node stepNode) {
        if (stepNode == null || stepNode.getType() == null) {
            return false;
        }
        return hasSupportedType(stepNode.getType(), new LinkedHashSet<String>());
    }

    private boolean hasSupportedType(Node typeNode, Set<String> visitedBlueIds) {
        if (typeNode == null) {
            return false;
        }
        boolean foundUnvisitedBlueId = false;
        for (String blueId : extractBlueIds(typeNode)) {
            if (SUPPORTED_BLUE_IDS.contains(blueId)) {
                return true;
            }
            if (visitedBlueIds.add(blueId)) {
                foundUnvisitedBlueId = true;
            }
        }
        Node parentType = typeNode.getType();
        if (parentType == null) {
            return false;
        }
        if (!foundUnvisitedBlueId) {
            List<String> parentBlueIds = extractBlueIds(parentType);
            boolean parentHasUnvisitedBlueId = false;
            for (String blueId : parentBlueIds) {
                if (!visitedBlueIds.contains(blueId)) {
                    parentHasUnvisitedBlueId = true;
                    break;
                }
            }
            if (!parentHasUnvisitedBlueId && parentBlueIds.isEmpty()) {
                return false;
            }
        }
        return hasSupportedType(parentType, visitedBlueIds);
    }

    private List<String> extractBlueIds(Node typeNode) {
        List<String> blueIds = new java.util.ArrayList<>();
        addBlueId(blueIds, typeNode.getBlueId());
        if (typeNode.getValue() instanceof String) {
            addBlueId(blueIds, String.valueOf(typeNode.getValue()));
        }
        if (typeNode.getProperties() != null && typeNode.getProperties().get("blueId") != null) {
            Node blueIdNode = typeNode.getProperties().get("blueId");
            if (blueIdNode != null && blueIdNode.getValue() != null) {
                addBlueId(blueIds, String.valueOf(blueIdNode.getValue()));
            }
        }
        return blueIds;
    }

    private void addBlueId(List<String> blueIds, String blueId) {
        if (blueId == null) {
            return;
        }
        String normalized = blueId.trim();
        if (normalized.isEmpty() || blueIds.contains(normalized)) {
            return;
        }
        blueIds.add(normalized);
    }
}
