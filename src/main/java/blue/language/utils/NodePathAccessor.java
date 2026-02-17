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
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        if (path.equals("/")) {
            return node.getValue() != null ? node.getValue() : node;
        }

        String[] rawSegments = path.substring(1).split("/", -1);
        String[] segments = new String[rawSegments.length];
        for (int i = 0; i < rawSegments.length; i++) {
            segments[i] = unescapePointerSegment(rawSegments[i]);
        }
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
        if (node == null) {
            throw new IllegalArgumentException("Property not found: " + segment);
        }
        Node result;

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
            switch (segment) {
                case "name":
                    result = new Node().value(node.getName());
                    break;
                case "description":
                    result = new Node().value(node.getDescription());
                    break;
                case "type":
                    result = node.getType();
                    break;
                case "itemType":
                    result = node.getItemType();
                    break;
                case "keyType":
                    result = node.getKeyType();
                    break;
                case "valueType":
                    result = node.getValueType();
                    break;
                case "value":
                    result = new Node().value(node.getValue());
                    break;
                case "blueId":
                    String blueId = node.getBlueId() != null
                            ? node.getBlueId()
                            : BlueIdCalculatorV2.calculateSemanticBlueId(node);
                    result = new Node().value(blueId);
                    break;
                case "blue":
                    result = node.getBlue();
                    break;
                default:
                    throw new IllegalArgumentException("Property not found: " + segment);
            }
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

    private static String unescapePointerSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return segment;
        }
        StringBuilder decoded = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c != '~') {
                decoded.append(c);
                continue;
            }
            if (i + 1 >= segment.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in segment: " + segment);
            }
            char next = segment.charAt(++i);
            if (next == '0') {
                decoded.append('~');
            } else if (next == '1') {
                decoded.append('/');
            } else {
                throw new IllegalArgumentException("Invalid JSON pointer escape in segment: " + segment);
            }
        }
        return decoded.toString();
    }

    private static Node link(Node node, Function<Node, Node> linkingProvider) {
        Node linked = linkingProvider.apply(node);
        return linked == null ? node : linked;
    }
}