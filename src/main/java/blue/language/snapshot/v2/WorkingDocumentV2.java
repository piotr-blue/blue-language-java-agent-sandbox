package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.util.PointerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorkingDocumentV2 {

    private final Blue blue;
    private final SnapshotFactoryV2 snapshotFactory;
    private final TypeGeneralizerV2 typeGeneralizer;

    private ResolvedSnapshotV2 baseline;
    private ResolvedSnapshotV2 current;
    private PatchReport lastPatchReport;

    private WorkingDocumentV2(Blue blue,
                              ResolvedSnapshotV2 snapshot,
                              SnapshotFactoryV2 snapshotFactory,
                              TypeGeneralizerV2 typeGeneralizer) {
        this.blue = Objects.requireNonNull(blue, "blue");
        this.baseline = Objects.requireNonNull(snapshot, "snapshot");
        this.current = snapshot;
        this.snapshotFactory = Objects.requireNonNull(snapshotFactory, "snapshotFactory");
        this.typeGeneralizer = Objects.requireNonNull(typeGeneralizer, "typeGeneralizer");
        this.lastPatchReport = PatchReport.none();
    }

    public static WorkingDocumentV2 forSnapshot(Blue blue, ResolvedSnapshotV2 snapshot) {
        return new WorkingDocumentV2(blue, snapshot, new SnapshotFactoryV2(), new TypeGeneralizerV2());
    }

    public ResolvedSnapshotV2 snapshot() {
        return current;
    }

    public PatchReport applyPatch(JsonPatch patch) {
        Objects.requireNonNull(patch, "patch");

        Node resolved = current.resolvedRoot().toNode();
        String normalizedPath = normalizeAndValidatePatchPointer(patch.getPath());
        validateMutationPath(normalizedPath, patch.getOp());
        applyPatchInPlace(resolved, patch, normalizedPath);

        GeneralizationReport generalizationReport = typeGeneralizer.generalizeToSoundness(blue, resolved, normalizedPath);
        current = snapshotFactory.fromResolved(blue, resolved, SnapshotTrustV2.BLIND_TRUST_RESOLVED);
        lastPatchReport = new PatchReport(Collections.singletonList(normalizedPath), generalizationReport);
        return lastPatchReport;
    }

    private String normalizeAndValidatePatchPointer(String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Patch path must be a JSON pointer starting with '/': " + path);
        }
        return PointerUtils.normalizePointer(path);
    }

    public ResolvedSnapshotV2 commit() {
        baseline = current;
        return current;
    }

    public PatchReport lastPatchReport() {
        return lastPatchReport;
    }

    private void applyPatchInPlace(Node root, JsonPatch patch, String normalizedPath) {
        if ("/".equals(normalizedPath)) {
            throw new IllegalArgumentException("Root patch is not supported in WorkingDocumentV2");
        }

        List<String> segments = splitPointer(normalizedPath);
        Node parent = resolveParent(root, segments, patch.getOp() != JsonPatch.Op.REMOVE, normalizedPath);
        String leaf = segments.get(segments.size() - 1);

        switch (patch.getOp()) {
            case ADD:
                applyAdd(parent, leaf, patch.getVal(), normalizedPath);
                break;
            case REPLACE:
                applyReplace(parent, leaf, patch.getVal(), normalizedPath);
                break;
            case REMOVE:
                applyRemove(parent, leaf, normalizedPath);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported op: " + patch.getOp());
        }
    }

    private void validateMutationPath(String normalizedPath, JsonPatch.Op op) {
        List<String> segments = splitPointer(normalizedPath);
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;

            if ("blueId".equals(segment) && last) {
                throw new UnsupportedOperationException("Mutating /blueId is forbidden in WorkingDocumentV2");
            }
            if ("type".equals(segment) && !last) {
                throw new UnsupportedOperationException("Mutating nested members under /type is forbidden");
            }
        }

        if (!segments.isEmpty() && "type".equals(segments.get(segments.size() - 1)) && op != JsonPatch.Op.REPLACE) {
            throw new UnsupportedOperationException("Only REPLACE is allowed for /type mutations");
        }
    }

    private void applyAdd(Node parent, String leaf, Node value, String path) {
        Node incoming = value != null ? value.clone() : new Node();
        List<Node> items = parent.getItems();
        if (items != null) {
            if ("-".equals(leaf)) {
                items.add(incoming);
                return;
            }
            int index = parseArrayIndex(leaf, path);
            if (index < 0 || index > items.size()) {
                throw new IllegalStateException("Array index out of bounds for add: " + path);
            }
            items.add(index, incoming);
            return;
        }

        if ("-".equals(leaf)) {
            throw new IllegalStateException("Append token '-' requires array parent at path: " + path);
        }

        ensureMutableProperties(parent).put(leaf, incoming);
    }

    private void applyReplace(Node parent, String leaf, Node value, String path) {
        Node incoming = value != null ? value.clone() : new Node();
        List<Node> items = parent.getItems();
        if (items != null) {
            if ("-".equals(leaf)) {
                throw new IllegalStateException("Replace does not support append token at path: " + path);
            }
            int index = parseArrayIndex(leaf, path);
            if (index < 0 || index >= items.size()) {
                throw new IllegalStateException("Array index out of bounds for replace: " + path);
            }
            items.set(index, incoming);
            return;
        }

        if ("-".equals(leaf)) {
            throw new IllegalStateException("Append token '-' requires array parent at path: " + path);
        }

        ensureMutableProperties(parent).put(leaf, incoming);
    }

    private void applyRemove(Node parent, String leaf, String path) {
        List<Node> items = parent.getItems();
        if (items != null) {
            if ("-".equals(leaf)) {
                throw new IllegalStateException("Remove does not support append token at path: " + path);
            }
            int index = parseArrayIndex(leaf, path);
            if (index < 0 || index >= items.size()) {
                throw new IllegalStateException("Array index out of bounds for remove: " + path);
            }
            items.remove(index);
            return;
        }

        if ("-".equals(leaf)) {
            throw new IllegalStateException("Append token '-' requires array parent at path: " + path);
        }

        Map<String, Node> properties = parent.getProperties();
        if (properties == null || !properties.containsKey(leaf)) {
            throw new IllegalStateException("Path does not exist for remove: " + path);
        }
        properties.remove(leaf);
    }

    private Node resolveParent(Node root, List<String> segments, boolean createMissing, String path) {
        Node currentNode = root;
        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            currentNode = descend(currentNode, segment, createMissing, path);
        }
        return currentNode;
    }

    private Node descend(Node currentNode, String segment, boolean createMissing, String path) {
        if (currentNode == null) {
            throw new IllegalStateException("Path does not exist: " + path);
        }

        List<Node> items = currentNode.getItems();
        if (items != null) {
            if ("-".equals(segment)) {
                throw new IllegalStateException("Append token '-' is only allowed on final segment: " + path);
            }
            int index = parseArrayIndex(segment, path);
            if (index < 0 || index >= items.size()) {
                throw new IllegalStateException("Array index out of bounds: " + path);
            }
            Node child = items.get(index);
            if (child == null && createMissing) {
                child = new Node();
                items.set(index, child);
            }
            return child;
        }

        Map<String, Node> properties = ensureMutableProperties(currentNode);
        Node child = properties.get(segment);
        if (child == null && createMissing) {
            child = new Node();
            properties.put(segment, child);
        }
        if (child == null) {
            throw new IllegalStateException("Path does not exist: " + path);
        }
        return child;
    }

    private List<String> splitPointer(String path) {
        if ("/".equals(path)) {
            return new ArrayList<String>();
        }

        String[] parts = path.substring(1).split("/", -1);
        List<String> result = new ArrayList<String>(parts.length);
        for (String part : parts) {
            result.add(unescapePointerSegment(part));
        }
        return result;
    }

    private String unescapePointerSegment(String segment) {
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

    private Map<String, Node> ensureMutableProperties(Node node) {
        Map<String, Node> properties = node.getProperties();
        if (properties == null) {
            node.properties(new LinkedHashMap<String, Node>());
            return node.getProperties();
        }
        if (!(properties instanceof LinkedHashMap)) {
            node.properties(new LinkedHashMap<String, Node>(properties));
            return node.getProperties();
        }
        return properties;
    }

    private int parseArrayIndex(String segment, String path) {
        if (!isArrayIndexSegment(segment)) {
            throw new IllegalStateException("Expected numeric array index in path: " + path);
        }
        try {
            int index = Integer.parseInt(segment);
            if (index < 0) {
                throw new IllegalStateException("Negative array index in path: " + path);
            }
            return index;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Expected numeric array index in path: " + path);
        }
    }

    private boolean isArrayIndexSegment(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return "0".equals(value) || value.charAt(0) != '0';
    }
}
