package blue.language.merge;

import blue.language.NodeProvider;
import blue.language.model.Node;
import blue.language.blueid.BlueIdCalculator;
import blue.language.utils.NodeExtender;
import blue.language.utils.NodeProviderWrapper;
import blue.language.utils.limits.Limits;
import blue.language.utils.limits.PathLimits;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Merger implements NodeResolver {

    private static final String MERGE_POLICY_POSITIONAL = "positional";
    private static final String MERGE_POLICY_APPEND_ONLY = "append-only";
    private static final String LIST_CONTROL_PREVIOUS = "$previous";
    private static final String LIST_CONTROL_POS = "$pos";

    private MergingProcessor mergingProcessor;
    private NodeProvider nodeProvider;

    public Merger(MergingProcessor mergingProcessor, NodeProvider nodeProvider) {
        this.mergingProcessor = mergingProcessor;
        this.nodeProvider = NodeProviderWrapper.wrap(nodeProvider);
    }

    public void merge(Node target, Node source, Limits limits) {
        if (source.getBlue() != null) {
            throw new IllegalArgumentException("Document contains \"blue\" attribute. Preprocess document before merging.");
        }

        if (source.getType() != null) {
            Node typeNode = source.getType();
            if (typeNode.getBlueId() != null) {
                new NodeExtender(nodeProvider).extend(typeNode, PathLimits.withSinglePath("/"));
            }

            Node resolvedType = resolve(typeNode, limits);
            source.type(resolvedType);
            merge(target, typeNode, limits);
        }
        mergeObject(target, source, limits);
    }

    private void mergeObject(Node target, Node source, Limits limits) {

        mergingProcessor.process(target, source, nodeProvider, this);
        if (source.getMergePolicy() != null) {
            target.mergePolicy(source.getMergePolicy());
        }

        List<Node> children = source.getItems();
        if (children != null) {
            mergeChildren(target, children, limits);
        }

        Map<String, Node> properties = source.getProperties();
        if (properties != null) {
            properties.forEach((key, value) -> {
                if (limits.shouldMergePathSegment(key, value)) {
                    limits.enterPathSegment(key, value);
                    mergeProperty(target, key, value, limits);
                    limits.exitPathSegment();
                }
            });
        }

        if (source.getBlueId() != null) {
            target.blueId(source.getBlueId());
        }

        mergingProcessor.postProcess(target, source, nodeProvider, this);
    }

    private void mergeChildren(Node target, List<Node> sourceChildren, Limits limits) {
        List<Node> targetChildren = target.getItems();
        List<Node> effectiveSourceChildren = applyListControls(sourceChildren, targetChildren);
        if (targetChildren == null) {
            targetChildren = effectiveSourceChildren.stream()
                    .filter(child -> limits.shouldMergePathSegment(String.valueOf(effectiveSourceChildren.indexOf(child)), target))
                    .map(child -> {
                        limits.enterPathSegment(String.valueOf(effectiveSourceChildren.indexOf(child)), target);
                        Node resolvedChild = resolve(child, limits);
                        limits.exitPathSegment();
                        return resolvedChild;
                    })
                    .collect(Collectors.toList());
            target.items(targetChildren);
            return;
        }

        String mergePolicy = target.getMergePolicy() == null ? MERGE_POLICY_POSITIONAL : target.getMergePolicy();
        if (!MERGE_POLICY_POSITIONAL.equals(mergePolicy) && !MERGE_POLICY_APPEND_ONLY.equals(mergePolicy)) {
            throw new IllegalArgumentException("Unknown mergePolicy: " + mergePolicy);
        }

        if (MERGE_POLICY_POSITIONAL.equals(mergePolicy) && effectiveSourceChildren.size() != targetChildren.size()) {
            throw new IllegalArgumentException(String.format(
                    "Positional mergePolicy requires the same number of items in subtype (%d) and supertype (%d).",
                    effectiveSourceChildren.size(), targetChildren.size()
            ));
        }

        if (effectiveSourceChildren.size() < targetChildren.size())
            throw new IllegalArgumentException(String.format(
                    "Subtype of element must not have more items (%d) than the element itself (%d).",
                    targetChildren.size(), effectiveSourceChildren.size()
            ));

        for (int i = 0; i < effectiveSourceChildren.size(); i++) {
            Node sourceChild = effectiveSourceChildren.get(i);
            if (!limits.shouldMergePathSegment(String.valueOf(i), sourceChild)) {
                continue;
            }
            limits.enterPathSegment(String.valueOf(i), sourceChild);
            if (i >= targetChildren.size()) {
                targetChildren.add(sourceChild);
                limits.exitPathSegment();
                continue;
            }
            String sourceBlueId = BlueIdCalculator.calculateSemanticBlueId(sourceChild, nodeProvider);
            String targetBlueId = BlueIdCalculator.calculateSemanticBlueId(targetChildren.get(i), nodeProvider);
            if (!sourceBlueId.equals(targetBlueId))
                throw new IllegalArgumentException(String.format(
                        "Mismatched items at index %d: source item has blueId '%s', but target item has blueId '%s'.",
                        i, sourceBlueId, targetBlueId
                ));
            limits.exitPathSegment();
        }
    }

    private List<Node> applyListControls(List<Node> sourceChildren, List<Node> inheritedChildren) {
        List<Node> effectiveChildren = new ArrayList<Node>();
        if (sourceChildren == null || sourceChildren.isEmpty()) {
            return effectiveChildren;
        }

        boolean hasControl = sourceChildren.stream().anyMatch(this::isListControlItem);
        if (!hasControl) {
            effectiveChildren.addAll(sourceChildren);
            return effectiveChildren;
        }

        int inheritedSize = inheritedChildren == null ? 0 : inheritedChildren.size();
        for (int i = 0; i < inheritedSize; i++) {
            effectiveChildren.add(inheritedChildren.get(i).clone());
        }

        boolean previousSeen = false;
        Set<Integer> usedPositions = new HashSet<Integer>();

        for (int i = 0; i < sourceChildren.size(); i++) {
            Node child = sourceChildren.get(i);
            ControlForm controlForm = parseControlForm(child);

            if (controlForm.previous) {
                if (i != 0) {
                    throw new IllegalArgumentException("$previous control form must be the first list item.");
                }
                if (inheritedSize == 0) {
                    throw new IllegalArgumentException("$previous control form requires inherited list items.");
                }
                previousSeen = true;
                continue;
            }

            if (controlForm.pos != null) {
                if (inheritedSize == 0) {
                    throw new IllegalArgumentException("$pos control form requires inherited list items.");
                }
                if (controlForm.pos < 0 || controlForm.pos >= inheritedSize) {
                    throw new IllegalArgumentException("$pos control form points outside inherited list range: " + controlForm.pos);
                }
                if (!usedPositions.add(controlForm.pos)) {
                    throw new IllegalArgumentException("Duplicate $pos control form for inherited index " + controlForm.pos + ".");
                }
                if (controlForm.content == null) {
                    throw new IllegalArgumentException("$pos control form must include item content.");
                }
                effectiveChildren.set(controlForm.pos, controlForm.content);
                continue;
            }

            effectiveChildren.add(child);
        }

        if (inheritedSize > 0 && !previousSeen) {
            throw new IllegalArgumentException("List control forms that modify inherited items must start with $previous.");
        }

        return effectiveChildren;
    }

    private boolean isListControlItem(Node child) {
        ControlForm form = parseControlForm(child);
        return form.previous || form.pos != null;
    }

    private ControlForm parseControlForm(Node child) {
        if (child == null || child.getProperties() == null || child.getProperties().isEmpty()) {
            return ControlForm.none();
        }

        Node previousNode = child.getProperties().get(LIST_CONTROL_PREVIOUS);
        Node posNode = child.getProperties().get(LIST_CONTROL_POS);

        boolean previous = previousNode != null && controlAsBoolean(previousNode);
        Integer pos = posNode == null ? null : controlAsInteger(posNode);

        if (!previous && pos == null) {
            return ControlForm.none();
        }
        if (previous && pos != null) {
            throw new IllegalArgumentException("List item cannot include both $previous and $pos control forms.");
        }

        Node content = stripControlProperties(child, previous, pos != null);
        if (previous && content != null) {
            throw new IllegalArgumentException("$previous control form cannot include item content.");
        }

        return new ControlForm(previous, pos, content);
    }

    private Node stripControlProperties(Node child, boolean removePrevious, boolean removePos) {
        Node content = child.clone();
        if (content.getProperties() == null) {
            return content;
        }

        if (removePrevious) {
            content.getProperties().remove(LIST_CONTROL_PREVIOUS);
        }
        if (removePos) {
            content.getProperties().remove(LIST_CONTROL_POS);
        }

        if (content.getProperties().isEmpty()) {
            content.properties((Map<String, Node>) null);
        }

        if (content.getName() == null &&
                content.getDescription() == null &&
                content.getType() == null &&
                content.getItemType() == null &&
                content.getKeyType() == null &&
                content.getValueType() == null &&
                content.getValue() == null &&
                content.getItems() == null &&
                content.getProperties() == null &&
                content.getBlueId() == null &&
                content.getConstraints() == null &&
                content.getBlue() == null) {
            return null;
        }
        return content;
    }

    private boolean controlAsBoolean(Node controlNode) {
        Object value = controlNode.getValue();
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new IllegalArgumentException("List control form value must be boolean: " + value);
    }

    private int controlAsInteger(Node controlNode) {
        Object value = controlNode.getValue();
        if (value instanceof BigInteger) {
            return ((BigInteger) value).intValueExact();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("List control form value must be integer: " + value);
    }

    private static final class ControlForm {
        private final boolean previous;
        private final Integer pos;
        private final Node content;

        private ControlForm(boolean previous, Integer pos, Node content) {
            this.previous = previous;
            this.pos = pos;
            this.content = content;
        }

        private static ControlForm none() {
            return new ControlForm(false, null, null);
        }
    }

    private void mergeProperty(Node target, String sourceKey, Node sourceValue, Limits limits) {
        Node node = resolve(sourceValue, limits);

        if (target.getProperties() == null)
            target.properties(new HashMap<>());
        Node targetValue = target.getProperties().get(sourceKey);
        if (targetValue == null)
            target.getProperties().put(sourceKey, node);
        else
            mergeObject(targetValue, node, limits);
    }

    @Override
    public Node resolve(Node node, Limits limits) {
        Node resultNode = new Node();
        merge(resultNode, node, limits);
        resultNode.name(node.getName());
        resultNode.description(node.getDescription());
        resultNode.blueId(node.getBlueId());
        return resultNode;
    }
}