package blue.language.utils;

import blue.language.model.Node;
import blue.language.blueid.v2.BlueIdCalculatorV2;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NodePathAccessor {

    public static Object get(Node node, String path) {
        return get(node, path, null);
    }
    
    public static Object get(Node node, String path, Function<Node, Node> linkingProvider) {
        return get(node, path, linkingProvider, true);
    }

    public static Object get(Node node, String path, Function<Node, Node> linkingProvider, boolean resolveFinalLink) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        if (path.equals("/")) {
            return node.getValue() != null ? node.getValue() : node;
        }

        String[] segments = path.substring(1).split("/");
        return getRecursive(node, segments, 0, linkingProvider, resolveFinalLink);
    }

    private static Object getRecursive(Node node, String[] segments, int index, Function<Node, Node> linkingProvider, boolean resolveFinalLink) {
        if (index == segments.length - 1 && !resolveFinalLink) {
            // Return the node itself for the last segment if we're not resolving the final link
            return getNodeForSegment(node, segments[index], linkingProvider, false);
        }

        if (index == segments.length) {
            return node != null && node.getValue() != null ? node.getValue() : node;
        }

        String segment = segments[index];
        Node nextNode = getNodeForSegment(node, segment, linkingProvider, true);
        return getRecursive(nextNode, segments, index + 1, linkingProvider, resolveFinalLink);
    }

    private static Node getNodeForSegment(Node node, String segment, Function<Node, Node> linkingProvider, boolean resolveLink) {
        Node result;

        switch (segment) {
            case "name":
                return new Node().value(node.getName());
            case "description":
                return new Node().value(node.getDescription());
            case "type":
                return node.getType();
            case "itemType":
                return node.getItemType();
            case "keyType":
                return node.getKeyType();
            case "valueType":
                return node.getValueType();
            case "value":
                return new Node().value(node.getValue());
            case "blueId":
                String blueId = node.getBlueId() != null
                        ? node.getBlueId()
                        : BlueIdCalculatorV2.calculateSemanticBlueId(node);
                return new Node().value(blueId);
        }

        Map<String, Node> properties = node.getProperties();
        if (properties != null && properties.containsKey(segment)) {
            result = properties.get(segment);
        } else if (isArrayIndexSegment(segment)) {
            int itemIndex = parseArrayIndex(segment);
            List<Node> items = node.getItems();
            if (items == null || itemIndex < 0 || itemIndex >= items.size()) {
                throw new IllegalArgumentException("Invalid item index: " + segment);
            }
            result = items.get(itemIndex);
        } else {
            throw new IllegalArgumentException("Property not found: " + segment);
        }

        return resolveLink && linkingProvider != null ? link(result, linkingProvider) : result;
    }

    private static boolean isArrayIndexSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return "0".equals(segment) || segment.charAt(0) != '0';
    }

    private static int parseArrayIndex(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static Node link(Node node, Function<Node, Node> linkingProvider) {
        Node linked = linkingProvider.apply(node);
        return linked == null ? node : linked;
    }
}