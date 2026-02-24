package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.util.PointerUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public static Node extend(Node template, Consumer<ExtendBuilder> customizer) {
        Node cloned = clone(template);
        ExtendBuilder extender = new ExtendBuilder(cloned);
        customizer.accept(extender);
        return extender.node();
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

    public static class ExtendBuilder {
        private final Node root;
        private final Map<String, String> participantTypeByKey = new LinkedHashMap<String, String>();

        private ExtendBuilder(Node root) {
            this.root = root;
        }

        public ExtendBuilder putDocumentValue(String key, Object value) {
            Node document = documentNode();
            if (value instanceof Node) {
                document.properties(key, (Node) value);
            } else {
                document.properties(key, new Node().value(value));
            }
            return this;
        }

        public ExtendBuilder putDocumentObject(String key, Consumer<NodeObjectBuilder> customizer) {
            NodeObjectBuilder builder = NodeObjectBuilder.create();
            customizer.accept(builder);
            documentNode().properties(key, builder.build());
            return this;
        }

        public ExtendBuilder bindAccount(String channelKey, String accountId) {
            ensureChannelBinding(channelKey).properties("accountId", new Node().value(accountId));
            return this;
        }

        public ExtendBuilder bindEmail(String channelKey, String email) {
            ensureChannelBinding(channelKey).properties("email", new Node().value(email));
            return this;
        }

        public ExtendBuilder applyPatch(JsonPatch patch) {
            applySinglePatch(root, patch);
            return this;
        }

        public ExtendBuilder applyPatches(List<JsonPatch> patches) {
            if (patches == null) {
                return this;
            }
            for (JsonPatch patch : patches) {
                applySinglePatch(root, patch);
            }
            return this;
        }

        public ExtendBuilder set(String pointer, Object value) {
            String path = normalizeDocumentPath(pointer);
            applySinglePatch(root, JsonPatch.replace(path, valueNode(value)));
            return this;
        }

        public ExtendBuilder participant(String channelKey) {
            return participant(channelKey, null, null);
        }

        public ExtendBuilder participant(String channelKey, String description) {
            return participant(channelKey, description, null);
        }

        public ExtendBuilder participant(String channelKey, String description, Node channel) {
            if (channelKey == null || channelKey.trim().isEmpty()) {
                throw new IllegalArgumentException("participant channel key is required");
            }
            Node nextChannel = channel == null
                    ? new Node().type(TypeAliases.CONVERSATION_TIMELINE_CHANNEL)
                    : channel;
            String nextTypeAlias = nextChannel.getAsText("/type/value");
            if (nextTypeAlias == null) {
                throw new IllegalArgumentException("participant channel type is required for: " + channelKey);
            }
            String existingAlias = existingChannelType(channelKey);
            if (existingAlias != null && !isTypeCompatible(existingAlias, nextTypeAlias)) {
                throw new IllegalArgumentException("Participant override is not type-compatible for channel: "
                        + channelKey + " (" + existingAlias + " -> " + nextTypeAlias + ")");
            }
            contractsMap().put(channelKey, nextChannel.clone());
            participantTypeByKey.put(channelKey, nextTypeAlias);
            if (description != null && !description.trim().isEmpty()) {
                Node labels = documentNode().getProperties() != null
                        ? documentNode().getProperties().get("participantLabels")
                        : null;
                if (labels == null) {
                    labels = new Node().properties(new LinkedHashMap<String, Node>());
                    documentNode().properties("participantLabels", labels);
                } else if (labels.getProperties() == null) {
                    labels.properties(new LinkedHashMap<String, Node>());
                }
                labels.getProperties().put(channelKey, new Node().value(description));
            }
            return this;
        }

        public ExtendBuilder participants(String... channelKeys) {
            if (channelKeys == null) {
                return this;
            }
            for (String channelKey : channelKeys) {
                participant(channelKey);
            }
            return this;
        }

        public ExtendBuilder participantsUnion(String compositeChannelKey, String... channelKeys) {
            contractsBuilder().compositeTimelineChannel(compositeChannelKey, channelKeys);
            return this;
        }

        public ExtendBuilder operation(String key,
                                       String channelKey,
                                       String description,
                                       Consumer<StepsBuilder> implementation) {
            return operation(key, channelKey, null, description, implementation);
        }

        public ExtendBuilder operation(String key,
                                       String channelKey,
                                       Class<?> requestTypeClass,
                                       String description,
                                       Consumer<StepsBuilder> implementation) {
            String resolvedChannelKey = ensureParticipantChannel(channelKey);
            if (requestTypeClass != null) {
                contractsBuilder().operation(key, resolvedChannelKey, requestTypeClass, description);
            } else {
                contractsBuilder().operation(key, resolvedChannelKey, description, request -> {
                });
            }
            contractsBuilder().implementOperation(key + "Impl", key, implementation);
            return this;
        }

        public ExtendBuilder onEvent(String workflowKey, Class<?> eventTypeClass, Consumer<StepsBuilder> customizer) {
            contractsBuilder().onTriggered(workflowKey, eventTypeClass, customizer);
            return this;
        }

        public ExtendBuilder onInit(String workflowKey, Consumer<StepsBuilder> customizer) {
            contractsBuilder().lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            contractsBuilder().onLifecycle(workflowKey, "initLifecycleChannel", customizer);
            return this;
        }

        public ExtendBuilder onDocChange(String workflowKey, String path, Consumer<StepsBuilder> customizer) {
            String channelKey = workflowKey + "Channel";
            contractsBuilder().documentUpdateChannel(channelKey, path);
            contractsBuilder().sequentialWorkflow(workflowKey,
                    channelKey,
                    new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE),
                    customizer);
            return this;
        }

        public ExtendBuilder directChangeWithAllowList(String operationName,
                                                       String channelKey,
                                                       String description,
                                                       String... allowedPaths) {
            String resolvedChannelKey = ensureParticipantChannel(channelKey);
            contractsBuilder().changeOperation(operationName, resolvedChannelKey, description, request -> {
            });
            contractsBuilder().changeWorkflowOperation(operationName + "Impl", operationName, steps -> steps
                    .js("CollectChangeset", BlueDocDsl.js(js -> js
                            .readRequest("request")
                            .returnOutput(JsOutputBuilder.output()
                                    .changesetRaw("request.changeset ?? []")
                                    .emptyEvents())))
                    .updateDocumentFromExpression("ApplyChangeset", "steps.CollectChangeset.changeset"));
            policiesBuilder().contractsChangePolicy("allow-listed-direct-change",
                    "operation constrained by explicit allow list");
            policiesBuilder().changesetAllowList(operationName, allowedPaths);
            return this;
        }

        public Node node() {
            return root;
        }

        protected Node documentNode() {
            if (root.getProperties() != null && root.getProperties().containsKey("document")) {
                return root.getAsNode("/document");
            }
            return root;
        }

        private ContractsBuilder contractsBuilder() {
            return new ContractsBuilder(contractsMap());
        }

        private PoliciesBuilder policiesBuilder() {
            return new PoliciesBuilder(policiesMap());
        }

        private Map<String, Node> contractsMap() {
            Node document = documentNode();
            Node contracts = document.getProperties() != null ? document.getProperties().get("contracts") : null;
            if (contracts == null) {
                contracts = new Node().properties(new LinkedHashMap<String, Node>());
                document.properties("contracts", contracts);
            } else if (contracts.getProperties() == null) {
                contracts.properties(new LinkedHashMap<String, Node>());
            }
            return contracts.getProperties();
        }

        private Map<String, Node> policiesMap() {
            Node document = documentNode();
            Node policies = document.getProperties() != null ? document.getProperties().get("policies") : null;
            if (policies == null) {
                policies = new Node().properties(new LinkedHashMap<String, Node>());
                document.properties("policies", policies);
            } else if (policies.getProperties() == null) {
                policies.properties(new LinkedHashMap<String, Node>());
            }
            return policies.getProperties();
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

        private String existingChannelType(String channelKey) {
            String alias = participantTypeByKey.get(channelKey);
            if (alias != null) {
                return alias;
            }
            Node existing = contractsMap().get(channelKey);
            if (existing == null) {
                return null;
            }
            return existing.getAsText("/type/value");
        }

        private String ensureParticipantChannel(String channelKey) {
            String resolved = resolveOperationChannelKey(channelKey);
            if (resolved == null || resolved.trim().isEmpty()) {
                throw new IllegalArgumentException("operation channel key is required");
            }
            if (!contractsMap().containsKey(resolved)) {
                participant(resolved);
            }
            return resolved;
        }

        private String resolveOperationChannelKey(String channelKey) {
            if (channelKey == null || channelKey.trim().isEmpty()) {
                return null;
            }
            String trimmed = channelKey.trim();
            if (contractsMap().containsKey(trimmed)) {
                return trimmed;
            }
            if (trimmed.endsWith("Channel")) {
                return trimmed;
            }
            String suffixed = trimmed + "Channel";
            if (contractsMap().containsKey(suffixed)) {
                return suffixed;
            }
            return suffixed;
        }

        private boolean isTypeCompatible(String existingAlias, String nextAlias) {
            if (existingAlias == null || nextAlias == null) {
                return false;
            }
            if (existingAlias.equals(nextAlias)) {
                return true;
            }
            if (TypeAliases.CORE_CHANNEL.equals(existingAlias)) {
                return TypeAliases.CONVERSATION_TIMELINE_CHANNEL.equals(nextAlias)
                        || TypeAliases.MYOS_TIMELINE_CHANNEL.equals(nextAlias);
            }
            if (TypeAliases.CONVERSATION_TIMELINE_CHANNEL.equals(existingAlias)) {
                return TypeAliases.MYOS_TIMELINE_CHANNEL.equals(nextAlias);
            }
            return false;
        }

        private String normalizeDocumentPath(String pointer) {
            if (pointer == null || pointer.trim().isEmpty()) {
                throw new IllegalArgumentException("Document pointer cannot be empty");
            }
            String trimmed = pointer.trim();
            boolean bootstrap = root.getProperties() != null && root.getProperties().containsKey("document");
            if (trimmed.startsWith("/document/") || trimmed.equals("/document")) {
                return trimmed;
            }
            if (!trimmed.startsWith("/")) {
                trimmed = "/" + trimmed;
            }
            return bootstrap ? "/document" + trimmed : trimmed;
        }

        private Node valueNode(Object value) {
            if (value instanceof Node) {
                return (Node) value;
            }
            return new Node().value(value);
        }
    }

    /**
     * Backward-compatible alias for previous naming.
     */
    @Deprecated
    public static final class DocumentMutator extends ExtendBuilder {
        private DocumentMutator(Node root) {
            super(root);
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
