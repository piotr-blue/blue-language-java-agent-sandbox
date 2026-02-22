package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.util.PointerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DocTemplates {

    private DocTemplates() {
    }

    public static DocTemplate template(Node bootstrap) {
        return DocTemplate.of(bootstrap);
    }

    public static Node clone(Node template) {
        if (template == null) {
            throw new IllegalArgumentException("template cannot be null");
        }
        return template.clone();
    }

    public static Node extend(Node template, Consumer<DocumentMutator> customizer) {
        Node cloned = clone(template);
        DocumentMutator mutator = new DocumentMutator(cloned);
        customizer.accept(mutator);
        return mutator.node();
    }

    public static Node applyPatch(Node template, List<JsonPatch> patchEntries) {
        Node cloned = clone(template);
        if (patchEntries == null) {
            return cloned;
        }
        for (JsonPatch patch : patchEntries) {
            applySinglePatch(cloned, patch);
        }
        return cloned;
    }

    public static final class DocumentMutator {
        private final Node root;

        private DocumentMutator(Node root) {
            this.root = root;
        }

        public DocumentMutator putDocumentValue(String key, Object value) {
            if (value instanceof Node) {
                root.getAsNode("/document").properties(key, (Node) value);
            } else {
                root.getAsNode("/document").properties(key, new Node().value(value));
            }
            return this;
        }

        public DocumentMutator putDocumentObject(String key, Consumer<NodeObjectBuilder> customizer) {
            NodeObjectBuilder builder = NodeObjectBuilder.create();
            customizer.accept(builder);
            root.getAsNode("/document").properties(key, builder.build());
            return this;
        }

        public DocumentMutator bindAccount(String channelKey, String accountId) {
            ensureChannelBinding(channelKey).properties("accountId", new Node().value(accountId));
            return this;
        }

        public DocumentMutator bindEmail(String channelKey, String email) {
            ensureChannelBinding(channelKey).properties("email", new Node().value(email));
            return this;
        }

        public DocumentMutator applyPatch(JsonPatch patch) {
            applySinglePatch(root, patch);
            return this;
        }

        public DocumentMutator applyPatches(List<JsonPatch> patches) {
            if (patches == null) {
                return this;
            }
            for (JsonPatch patch : patches) {
                applySinglePatch(root, patch);
            }
            return this;
        }

        public Node node() {
            return root;
        }

        private Node ensureChannelBinding(String channelKey) {
            Node bindings = root.getProperties() != null ? root.getProperties().get("channelBindings") : null;
            if (bindings == null) {
                bindings = new Node();
                root.properties("channelBindings", bindings);
            }
            Node binding = bindings.getProperties() != null ? bindings.getProperties().get(channelKey) : null;
            if (binding == null) {
                binding = new Node();
                bindings.properties(channelKey, binding);
            }
            return binding;
        }
    }

    private static void applySinglePatch(Node root, JsonPatch patch) {
        if (patch == null) {
            return;
        }
        String path = PointerUtils.normalizeRequiredPointer(patch.getPath(), "Patch path");
        List<String> segments = PointerUtils.splitPointerSegmentsList(path);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Root patch is not supported: " + path);
        }

        Node parent = root;
        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            parent = descendOrCreate(parent, segment, path);
        }
        String leaf = segments.get(segments.size() - 1);
        switch (patch.getOp()) {
            case ADD:
            case REPLACE:
                setValue(parent, leaf, patch.getVal() != null ? patch.getVal().clone() : new Node(), path);
                break;
            case REMOVE:
                removeValue(parent, leaf, path);
                break;
            default:
                throw new IllegalArgumentException("Unsupported patch op: " + patch.getOp());
        }
    }

    private static Node descendOrCreate(Node parent, String segment, String path) {
        if (PointerUtils.isArrayIndexSegment(segment)) {
            List<Node> items = parent.getItems();
            if (items == null) {
                throw new IllegalStateException("Array segment used on non-array parent: " + path);
            }
            int index = PointerUtils.parseArrayIndexOrThrow(segment, path);
            if (index < 0 || index >= items.size()) {
                throw new IllegalStateException("Array index out of bounds: " + path);
            }
            Node child = items.get(index);
            if (child == null) {
                child = new Node();
                items.set(index, child);
            }
            return child;
        }
        Map<String, Node> properties = parent.getProperties();
        if (properties == null) {
            parent.properties(new java.util.LinkedHashMap<String, Node>());
            properties = parent.getProperties();
        }
        Node child = properties.get(segment);
        if (child == null) {
            child = new Node();
            properties.put(segment, child);
        }
        return child;
    }

    private static void setValue(Node parent, String leaf, Node value, String path) {
        if (PointerUtils.isArrayIndexSegment(leaf)) {
            List<Node> items = parent.getItems();
            if (items == null) {
                parent.items(new ArrayList<Node>());
                items = parent.getItems();
            }
            int index = PointerUtils.parseArrayIndexOrThrow(leaf, path);
            while (items.size() <= index) {
                items.add(new Node());
            }
            items.set(index, value);
            return;
        }
        Map<String, Node> properties = parent.getProperties();
        if (properties == null) {
            parent.properties(new java.util.LinkedHashMap<String, Node>());
            properties = parent.getProperties();
        }
        properties.put(leaf, value);
    }

    private static void removeValue(Node parent, String leaf, String path) {
        if (PointerUtils.isArrayIndexSegment(leaf)) {
            List<Node> items = parent.getItems();
            if (items == null) {
                throw new IllegalStateException("Cannot remove from non-array path: " + path);
            }
            int index = PointerUtils.parseArrayIndexOrThrow(leaf, path);
            if (index < 0 || index >= items.size()) {
                throw new IllegalStateException("Array index out of bounds: " + path);
            }
            items.remove(index);
            return;
        }
        Map<String, Node> properties = parent.getProperties();
        if (properties == null || !properties.containsKey(leaf)) {
            throw new IllegalStateException("Path does not exist for remove: " + path);
        }
        properties.remove(leaf);
    }
}
