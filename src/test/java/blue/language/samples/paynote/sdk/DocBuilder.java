package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.processor.util.PointerUtils;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.dsl.ContractsBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.MyOsBootstrapBuilder;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PoliciesBuilder;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.dsl.TypeRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DocBuilder<T extends DocBuilder<T>> {

    private final Node document;
    private final Map<String, Node> contracts = new LinkedHashMap<String, Node>();
    private final Map<String, Node> policies = new LinkedHashMap<String, Node>();
    private final Map<String, Node> participantLabels = new LinkedHashMap<String, Node>();

    protected DocBuilder() {
        this.document = new Node();
    }

    protected DocBuilder(Node existingDocument) {
        if (existingDocument == null) {
            throw new IllegalArgumentException("existingDocument is required");
        }
        this.document = existingDocument.clone();
        detachObjectProperty("contracts", contracts);
        detachObjectProperty("policies", policies);
        detachObjectProperty("participantLabels", participantLabels);
    }

    public static SimpleDocBuilder edit(Node existingDocument) {
        return SimpleDocBuilder.edit(existingDocument);
    }

    public static SimpleDocBuilder from(Node existingDocument) {
        return edit(existingDocument);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    protected void beforeOperation(String channelKey) {
        // Override in subclasses that need channel-specific preconditions.
    }

    protected final void contracts(Consumer<ContractsBuilder> customizer) {
        ContractsBuilder builder = new ContractsBuilder(contracts);
        customizer.accept(builder);
    }

    protected final void policies(Consumer<PoliciesBuilder> customizer) {
        PoliciesBuilder builder = new PoliciesBuilder(policies);
        customizer.accept(builder);
    }

    public T type(String documentTypeAlias) {
        document.type(documentTypeAlias);
        return self();
    }

    public T withName(String name) {
        document.name(name);
        return self();
    }

    public T description(String description) {
        document.description(description);
        return self();
    }

    public T participant(String channelKey) {
        return participant(channelKey, null, null);
    }

    public T participant(ChannelKey channelKey) {
        return participant(channelKey.value(), null, null);
    }

    public T participant(String channelKey, String label) {
        return participant(channelKey, label, null);
    }

    public T participant(ChannelKey channelKey, String label) {
        return participant(channelKey.value(), label, null);
    }

    public T participant(String channelKey, String label, Node channel) {
        String key = requireChannelKey(channelKey, "participant channel key is required");
        if (channel != null) {
            contracts(c -> c.putRaw(key, channel));
        } else {
            contracts(c -> c.timelineChannel(key));
        }
        if (label != null && !label.trim().isEmpty()) {
            participantLabels.put(key, new Node().value(label));
        }
        return self();
    }

    public T participant(ChannelKey channelKey, String label, Node channel) {
        return participant(channelKey.value(), label, channel);
    }

    public T participants(String... channelKeys) {
        if (channelKeys == null) {
            return self();
        }
        for (String channelKey : channelKeys) {
            participant(channelKey);
        }
        return self();
    }

    public T participantsUnion(String compositeChannelKey, String... channelKeys) {
        return participantsUnion(ChannelKey.of(compositeChannelKey), channelKeys);
    }

    public T participantsUnion(ChannelKey compositeChannelKey, String... channelKeys) {
        contracts(c -> c.compositeTimelineChannel(compositeChannelKey.value(), channelKeys));
        return self();
    }

    public T set(String pointer, Object value) {
        setPointer(pointer, toNode(value));
        return self();
    }

    public T replace(String pointer, Object value) {
        setPointer(pointer, toNode(value));
        return self();
    }

    public T remove(String pointer) {
        removePointer(pointer);
        return self();
    }

    public T operation(String key,
                       String channelKey,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        return operation(key, channelKey, null, description, implementation);
    }

    public T operation(String key,
                       ChannelKey channelKey,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        return operation(key, channelKey.value(), null, description, implementation);
    }

    public T operation(String key,
                       String channelKey,
                       Class<?> requestTypeClass,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        String normalizedChannelKey = requireChannelKey(channelKey, "operation channel key is required");
        beforeOperation(normalizedChannelKey);
        contracts(c -> {
            if (requestTypeClass != null) {
                c.operation(key, normalizedChannelKey, requestTypeClass, description);
            } else {
                c.operation(key, normalizedChannelKey, description);
            }
            c.implementOperation(key + "Impl", key, implementation);
        });
        return self();
    }

    public T operation(String key,
                       ChannelKey channelKey,
                       Class<?> requestTypeClass,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        return operation(key, channelKey.value(), requestTypeClass, description, implementation);
    }

    public OperationBuilder<T> operation(String key) {
        return new OperationBuilder<T>(self(), key);
    }

    public T onEvent(String workflowKey, Class<?> eventTypeClass, Consumer<StepsBuilder> customizer) {
        contracts(c -> c.onTriggered(workflowKey, eventTypeClass, customizer));
        return self();
    }

    public T onDocChange(String workflowKey, String path, Consumer<StepsBuilder> customizer) {
        String channelKey = workflowKey + "Channel";
        contracts(c -> {
            c.documentUpdateChannel(channelKey, path);
            c.sequentialWorkflow(workflowKey,
                    channelKey,
                    new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE),
                    customizer);
        });
        return self();
    }

    public T onInit(String workflowKey, Consumer<StepsBuilder> customizer) {
        contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle(workflowKey, "initLifecycleChannel", customizer);
        });
        return self();
    }

    public T myOsAdmin(String channelKey) {
        String normalized = requireChannelKey(channelKey, "myOs admin channel key is required");
        participant(normalized, "MyOS admin");
        operation("myOsAdminUpdate", normalized, "MyOS admin update", steps -> steps
                .js("EmitAdminEvents", BlueDocDsl.js(js -> js.returnOutput(
                        JsOutputBuilder.output().eventsRaw("event.message.request")))));
        return self();
    }

    public T onMyOsResponse(String workflowKey,
                            Class<?> responseTypeClass,
                            String requestId,
                            Consumer<StepsBuilder> customizer) {
        Node event = new Node().type(TypeRef.of(responseTypeClass).asTypeNode());
        if (requestId != null && !requestId.trim().isEmpty()) {
            event.properties("requestId", new Node().value(requestId.trim()));
        }
        contracts(c -> c.onTriggered(workflowKey, event, customizer));
        return self();
    }

    public T onMyOsResponse(String workflowKey,
                            Class<?> responseTypeClass,
                            Consumer<StepsBuilder> customizer) {
        return onMyOsResponse(workflowKey, responseTypeClass, null, customizer);
    }

    public T directChangeWithAllowList(String operationName,
                                       String channelKey,
                                       String description,
                                       String... allowedPaths) {
        operation(operationName, channelKey, description, steps -> steps
                .js("CollectChangeset", BlueDocDsl.js(js -> js
                        .readRequest("request")
                        .returnOutput(JsOutputBuilder.output()
                                .changesetRaw("request.changeset ?? []")
                                .emptyEvents())))
                .updateDocumentFromExpression("ApplyChangeset", "steps.CollectChangeset.changeset"));

        policies(p -> p
                .contractsChangePolicy("allow-listed-direct-change", "operation constrained by explicit allow list")
                .changesetAllowList(operationName, allowedPaths));
        return self();
    }

    public Node buildDocument() {
        Node built = document.clone();
        if (!participantLabels.isEmpty()) {
            built.properties("participantLabels", new Node().properties(cloneNodeMap(participantLabels)));
        }
        if (!contracts.isEmpty()) {
            built.properties("contracts", new Node().properties(cloneNodeMap(contracts)));
        }
        if (!policies.isEmpty()) {
            built.properties("policies", new Node().properties(cloneNodeMap(policies)));
        }
        return built;
    }

    public Node buildBootstrap() {
        return bootstrap().build();
    }

    public MyOsBootstrapBuilder bootstrap() {
        return MyOsDsl.bootstrap(buildDocument());
    }

    public static String expr(String expression) {
        if (expression == null) {
            return null;
        }
        String trimmed = expression.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "${" + trimmed + "}";
    }

    private static String requireChannelKey(String channelKey, String message) {
        if (channelKey == null || channelKey.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return channelKey.trim();
    }

    private static Map<String, Node> cloneNodeMap(Map<String, Node> source) {
        Map<String, Node> out = new LinkedHashMap<String, Node>();
        for (Map.Entry<String, Node> entry : source.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
        return out;
    }

    private void detachObjectProperty(String key, Map<String, Node> target) {
        if (document.getProperties() == null) {
            return;
        }
        Node existing = document.getProperties().remove(key);
        if (existing == null || existing.getProperties() == null) {
            return;
        }
        for (Map.Entry<String, Node> entry : existing.getProperties().entrySet()) {
            target.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
        }
    }

    private static Node toNode(Object value) {
        if (value instanceof Node) {
            return ((Node) value).clone();
        }
        return new Node().value(value);
    }

    private void setPointer(String pointer, Node valueNode) {
        String normalized = PointerUtils.normalizeRequiredPointer(pointer, "pointer");
        if ("/".equals(normalized)) {
            throw new IllegalArgumentException("pointer cannot target root");
        }
        List<String> segments = PointerUtils.splitPointerSegmentsList(normalized);
        Node current = document;
        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            String nextSegment = segments.get(i + 1);
            current = descendOrCreate(current, segment, nextSegment, normalized);
        }
        String leaf = segments.get(segments.size() - 1);
        assign(current, leaf, valueNode.clone(), normalized);
    }

    private void removePointer(String pointer) {
        String normalized = PointerUtils.normalizeRequiredPointer(pointer, "pointer");
        if ("/".equals(normalized)) {
            throw new IllegalArgumentException("pointer cannot target root");
        }
        List<String> segments = PointerUtils.splitPointerSegmentsList(normalized);
        Node current = document;
        for (int i = 0; i < segments.size() - 1; i++) {
            current = descendExisting(current, segments.get(i), normalized);
            if (current == null) {
                return;
            }
        }
        String leaf = segments.get(segments.size() - 1);
        if (current.getItems() != null) {
            int index = PointerUtils.parseArrayIndex(leaf);
            if (index >= 0 && index < current.getItems().size()) {
                current.getItems().remove(index);
            }
            return;
        }
        if (current.getProperties() != null) {
            current.getProperties().remove(leaf);
        }
    }

    private Node descendOrCreate(Node current,
                                 String segment,
                                 String nextSegment,
                                 String fullPath) {
        if (current.getItems() != null) {
            int index = PointerUtils.parseArrayIndex(segment);
            if (index < 0) {
                throw new IllegalArgumentException("Expected numeric array segment in path: " + fullPath);
            }
            List<Node> items = current.getItems();
            while (items.size() <= index) {
                items.add(new Node());
            }
            Node child = items.get(index);
            if (child == null) {
                child = new Node();
                items.set(index, child);
            }
            ensureContainerForNextSegment(child, nextSegment);
            return child;
        }
        Map<String, Node> properties = ensureProperties(current);
        Node child = properties.get(segment);
        if (child == null) {
            child = new Node();
            properties.put(segment, child);
        }
        ensureContainerForNextSegment(child, nextSegment);
        return child;
    }

    private Node descendExisting(Node current, String segment, String fullPath) {
        if (current.getItems() != null) {
            int index = PointerUtils.parseArrayIndex(segment);
            if (index < 0) {
                throw new IllegalArgumentException("Expected numeric array segment in path: " + fullPath);
            }
            if (index >= current.getItems().size()) {
                return null;
            }
            return current.getItems().get(index);
        }
        if (current.getProperties() == null) {
            return null;
        }
        return current.getProperties().get(segment);
    }

    private void assign(Node current, String leaf, Node value, String fullPath) {
        if (current.getItems() != null) {
            int index = PointerUtils.parseArrayIndex(leaf);
            if (index < 0) {
                throw new IllegalArgumentException("Expected numeric array segment in path: " + fullPath);
            }
            List<Node> items = current.getItems();
            while (items.size() <= index) {
                items.add(new Node());
            }
            items.set(index, value);
            return;
        }
        ensureProperties(current).put(leaf, value);
    }

    private void ensureContainerForNextSegment(Node node, String nextSegment) {
        if (node.getProperties() != null || node.getItems() != null) {
            return;
        }
        if (PointerUtils.isArrayIndexSegment(nextSegment)) {
            node.items(new ArrayList<Node>());
        } else {
            node.properties(new LinkedHashMap<String, Node>());
        }
    }

    private Map<String, Node> ensureProperties(Node node) {
        if (node.getProperties() == null) {
            node.properties(new LinkedHashMap<String, Node>());
        }
        return node.getProperties();
    }

    public static final class OperationBuilder<P extends DocBuilder<P>> {
        private final P parent;
        private final String key;
        private String channelKey;
        private String description;
        private Class<?> requestTypeClass;
        private Consumer<StepsBuilder> implementation;

        private OperationBuilder(P parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        public OperationBuilder<P> channel(String channelKey) {
            this.channelKey = channelKey;
            return this;
        }

        public OperationBuilder<P> channel(ChannelKey channelKey) {
            this.channelKey = channelKey.value();
            return this;
        }

        public OperationBuilder<P> description(String description) {
            this.description = description;
            return this;
        }

        public OperationBuilder<P> requestType(Class<?> requestTypeClass) {
            this.requestTypeClass = requestTypeClass;
            return this;
        }

        public OperationBuilder<P> noRequest() {
            this.requestTypeClass = null;
            return this;
        }

        public OperationBuilder<P> steps(Consumer<StepsBuilder> implementation) {
            this.implementation = implementation;
            return this;
        }

        public P done() {
            if (channelKey == null || channelKey.trim().isEmpty()) {
                throw new IllegalStateException("Operation channel must be configured for: " + key);
            }
            if (implementation == null) {
                throw new IllegalStateException("Operation steps must be configured for: " + key);
            }
            if (requestTypeClass != null) {
                return parent.operation(key, channelKey, requestTypeClass, description, implementation);
            }
            return parent.operation(key, channelKey, description, implementation);
        }
    }
}
