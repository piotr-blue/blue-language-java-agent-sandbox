package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TypeGeneralizerV2 {

    public GeneralizationReport generalizeToSoundness(Blue blue, FrozenNode resolvedRoot, String changedPointer) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolvedRoot, "resolvedRoot");
        Node mutableRoot = resolvedRoot.toNode();
        return generalizeToSoundness(blue, mutableRoot, changedPointer);
    }

    public GeneralizationReport generalizeToSoundness(Blue blue, Node mutableResolvedRoot, String changedPointer) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(mutableResolvedRoot, "mutableResolvedRoot");

        List<String> records = new ArrayList<String>();
        List<String> pointers = parentPointers(changedPointer);
        for (String pointer : pointers) {
            Node node = nodeAt(mutableResolvedRoot, pointer);
            if (node == null || node.getType() == null) {
                continue;
            }

            while (!isConformant(blue, node)) {
                Node currentType = node.getType();
                Node parentType = currentType.getType();
                if (parentType == null) {
                    break;
                }

                String before = displayType(currentType);
                Node generalizedType = toTypeReference(parentType);
                node.type(generalizedType);
                String after = displayType(generalizedType);
                records.add(pointer + ": " + before + " -> " + after);
            }
        }

        if (records.isEmpty()) {
            return GeneralizationReport.none();
        }
        return new GeneralizationReport(records);
    }

    private boolean isConformant(Blue blue, Node node) {
        try {
            blue.resolve(node.clone());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private List<String> parentPointers(String changedPointer) {
        String normalized = normalizePointer(changedPointer);
        if ("/".equals(normalized)) {
            List<String> rootOnly = new ArrayList<String>();
            rootOnly.add("/");
            return rootOnly;
        }

        List<String> pointers = new ArrayList<String>();
        String current = normalized;
        while (true) {
            int idx = current.lastIndexOf('/');
            if (idx <= 0) {
                pointers.add("/");
                break;
            }
            current = current.substring(0, idx);
            pointers.add(current);
            if ("/".equals(current)) {
                break;
            }
        }
        return pointers;
    }

    private Node nodeAt(Node root, String pointer) {
        if ("/".equals(pointer)) {
            return root;
        }

        String[] segments = pointer.substring(1).split("/", -1);
        Node current = root;
        for (String rawSegment : segments) {
            String segment = unescape(rawSegment);
            Map<String, Node> properties = current.getProperties();
            if (properties != null && properties.containsKey(segment)) {
                current = properties.get(segment);
            } else if (isArrayIndexSegment(segment)) {
                if (current.getItems() == null) {
                    return null;
                }
                int index = parseArrayIndex(segment);
                if (index < 0 || index >= current.getItems().size()) {
                    return null;
                }
                current = current.getItems().get(index);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Node toTypeReference(Node typeNode) {
        if (typeNode.getBlueId() != null) {
            return new Node().blueId(typeNode.getBlueId());
        }
        return typeNode.clone();
    }

    private String displayType(Node typeNode) {
        if (typeNode == null) {
            return "<none>";
        }
        if (typeNode.getName() != null) {
            return typeNode.getName();
        }
        if (typeNode.getBlueId() != null) {
            return typeNode.getBlueId();
        }
        return "<anonymous>";
    }

    private String normalizePointer(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return "/";
        }
        if (pointer.charAt(0) != '/') {
            throw new IllegalArgumentException("Invalid JSON pointer: " + pointer);
        }
        validatePointerEscapes(pointer);
        return pointer;
    }

    private void validatePointerEscapes(String pointer) {
        for (int i = 1; i < pointer.length(); i++) {
            char c = pointer.charAt(i);
            if (c != '~') {
                continue;
            }
            if (i + 1 >= pointer.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
            char next = pointer.charAt(++i);
            if (next != '0' && next != '1') {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
        }
    }

    private String unescape(String segment) {
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

    private boolean isArrayIndexSegment(String segment) {
        if (!isNonNegativeInteger(segment)) {
            return false;
        }
        return "0".equals(segment) || segment.charAt(0) != '0';
    }

    private boolean isNonNegativeInteger(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private int parseArrayIndex(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
