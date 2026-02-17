package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.util.PointerUtils;

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
        String[] segments = PointerUtils.splitPointerSegments(changedPointer);
        if (segments.length == 0) {
            List<String> rootOnly = new ArrayList<String>();
            rootOnly.add("/");
            return rootOnly;
        }

        List<String> pointers = new ArrayList<String>();
        for (int length = segments.length - 1; length >= 0; length--) {
            pointers.add(PointerUtils.pointerFromSegments(segments, length));
        }
        return pointers;
    }

    private Node nodeAt(Node root, String pointer) {
        String[] segments = PointerUtils.splitPointerSegments(pointer);
        Node current = root;
        for (String segment : segments) {
            Map<String, Node> properties = current.getProperties();
            if (properties != null && properties.containsKey(segment)) {
                current = properties.get(segment);
            } else if (PointerUtils.isArrayIndexSegment(segment)) {
                if (current.getItems() == null) {
                    return null;
                }
                int index = PointerUtils.parseArrayIndex(segment);
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

}
