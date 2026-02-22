package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.script.QuickJSEvaluator;
import blue.language.processor.script.QuickJsExpressionUtils;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateDocumentStepExecutor implements WorkflowStepExecutor {

    private static final Set<String> SUPPORTED_BLUE_IDS =
            Collections.unmodifiableSet(new LinkedHashSet<String>(java.util.Arrays.asList(
                    "Conversation/Update Document",
                    "UpdateDocument"
            )));

    private final QuickJSEvaluator evaluator;

    public UpdateDocumentStepExecutor() {
        this(new QuickJSEvaluator());
    }

    public UpdateDocumentStepExecutor(QuickJSEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Set<String> supportedBlueIds() {
        return SUPPORTED_BLUE_IDS;
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        if (!isValidStepNode(args.stepNode())) {
            args.context().throwFatal("Update Document step payload is invalid");
            return null;
        }
        Node stepNode = QuickJsExpressionUtils.resolveExpressions(
                args.stepNode(),
                evaluator,
                QuickJSStepBindings.create(args),
                args.context(),
                QuickJsExpressionUtils.createPathPredicate(
                        java.util.Arrays.asList("/changeset", "/changeset/**"),
                        null),
                null);
        List<Node> changes = readChangeset(stepNode);
        args.context().chargeUpdateDocumentBase(changes.size());
        for (Node change : changes) {
            applyChange(args, change);
        }
        return null;
    }

    private List<Node> readChangeset(Node stepNode) {
        if (stepNode == null || stepNode.getProperties() == null) {
            return Collections.emptyList();
        }
        Node changesetNode = stepNode.getProperties().get("changeset");
        if (changesetNode == null || changesetNode.getItems() == null) {
            return Collections.emptyList();
        }
        return changesetNode.getItems();
    }

    private void applyChange(StepExecutionArgs args, Node change) {
        if (change == null || change.getProperties() == null) {
            args.context().throwFatal("Update Document changeset entry must be an object");
            return;
        }
        String op = readString(change, "op");
        if (op == null || op.trim().isEmpty()) {
            op = "REPLACE";
        }
        String normalizedOp = op.trim().toUpperCase(Locale.ROOT);
        String path = readString(change, "path");
        if (path == null || path.trim().isEmpty()) {
            args.context().throwFatal("Update Document changeset requires a non-empty path");
            return;
        }
        String absolutePath = args.context().resolvePointer(path);

        if ("REMOVE".equals(normalizedOp)) {
            args.context().applyPatch(JsonPatch.remove(absolutePath));
            return;
        }

        Node valueNode = change.getProperties().get("val");
        if (valueNode == null) {
            args.context().throwFatal("Update Document " + normalizedOp + " operation requires val");
            return;
        }
        if ("ADD".equals(normalizedOp)) {
            args.context().applyPatch(JsonPatch.add(absolutePath, valueNode.clone()));
            return;
        }
        if ("REPLACE".equals(normalizedOp)) {
            args.context().applyPatch(JsonPatch.replace(absolutePath, valueNode.clone()));
            return;
        }
        args.context().throwFatal("Unsupported Update Document operation \"" + op + "\"");
    }

    private String readString(Node node, String key) {
        if (node.getProperties() == null) {
            return null;
        }
        Node valueNode = node.getProperties().get(key);
        if (valueNode == null || valueNode.getValue() == null) {
            return null;
        }
        return String.valueOf(valueNode.getValue());
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
