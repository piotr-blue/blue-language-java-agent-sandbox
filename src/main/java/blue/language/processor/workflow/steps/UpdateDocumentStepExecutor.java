package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UpdateDocumentStepExecutor implements WorkflowStepExecutor {

    @Override
    public Set<String> supportedBlueIds() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
                "Conversation/Update Document",
                "UpdateDocument"
        )));
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        Node stepNode = args.stepNode();
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
}
