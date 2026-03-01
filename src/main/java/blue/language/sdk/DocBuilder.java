package blue.language.sdk;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.util.PointerUtils;
import blue.language.sdk.internal.ContractsBuilder;
import blue.language.sdk.internal.PoliciesBuilder;
import blue.language.sdk.internal.StepsBuilder;
import blue.language.sdk.internal.TypeAliases;
import blue.language.sdk.internal.TypeRef;
import blue.language.types.core.Channel;
import blue.language.types.myos.SubscriptionUpdate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DocBuilder<T extends DocBuilder<T>> {

    private static final Blue BLUE = new Blue();

    protected final Node document;
    private String activeSectionKey;

    protected DocBuilder() {
        this.document = new Node();
    }

    protected DocBuilder(Node existingDocument) {
        require(existingDocument, "existing document");
        this.document = existingDocument;
    }

    public static SimpleDocBuilder doc() {
        return SimpleDocBuilder.doc();
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

    public T name(String name) {
        document.name(name);
        return self();
    }

    public T description(String description) {
        document.description(description);
        return self();
    }

    public T type(String typeAlias) {
        document.type(typeAlias);
        return self();
    }

    public T type(Class<?> typeClass) {
        require(typeClass, "type class");
        document.type(TypeRef.of(typeClass).asTypeNode());
        return self();
    }

    public T section(String key, String title, String summary) {
        require(key, "section key");
        Node sectionNode = ensureSectionContract(key);
        if (title != null) {
            sectionNode.properties("title", new Node().value(title));
        }
        if (summary != null) {
            sectionNode.properties("summary", new Node().value(summary));
        }
        ensureSectionListNode(sectionNode, "relatedFields");
        ensureSectionListNode(sectionNode, "relatedContracts");
        activeSectionKey = key;
        return self();
    }

    public T section(String key) {
        require(key, "section key");
        Node sectionNode = ensureSectionContract(key);
        ensureSectionListNode(sectionNode, "relatedFields");
        ensureSectionListNode(sectionNode, "relatedContracts");
        activeSectionKey = key;
        return self();
    }

    public T endSection() {
        activeSectionKey = null;
        return self();
    }

    public T channel(String channelKey) {
        require(channelKey, "channel key");
        contracts().timelineChannel(channelKey);
        addContractToActiveSection(channelKey);
        return self();
    }

    public T channel(String channelKey, Channel channelContract) {
        require(channelKey, "channel key");
        require(channelContract, "channel contract");
        Node channelNode = BLUE.objectToNode(channelContract);
        pruneEmptyEventProperty(channelNode);
        channelNode.type(TypeRef.of(channelContract.getClass()).alias());
        contracts().putRaw(channelKey, channelNode);
        addContractToActiveSection(channelKey);
        return self();
    }

    public T channels(String... channelKeys) {
        if (channelKeys == null) {
            return self();
        }
        for (String channelKey : channelKeys) {
            channel(channelKey);
        }
        return self();
    }

    public T compositeChannel(String compositeChannelKey, String... channelKeys) {
        require(compositeChannelKey, "composite channel key");
        contracts().compositeTimelineChannel(compositeChannelKey, channelKeys);
        addContractToActiveSection(compositeChannelKey);
        return self();
    }

    public T operation(String key,
                       String channelKey,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        return operation(key, channelKey, null, description, implementation);
    }

    public T operation(String key,
                       String channelKey,
                       Class<?> requestTypeClass,
                       String description,
                       Consumer<StepsBuilder> implementation) {
        require(key, "operation key");
        require(channelKey, "channel key");
        require(implementation, "steps");
        ContractsBuilder contracts = contracts();
        if (requestTypeClass == null) {
            contracts.operation(key, channelKey, description);
        } else {
            contracts.operation(key, channelKey, requestTypeClass, description);
        }
        contracts.implementOperation(key + "Impl", key, implementation);
        addContractToActiveSection(key);
        addContractToActiveSection(key + "Impl");
        return self();
    }

    public OperationBuilder<T> operation(String key) {
        return new OperationBuilder<T>(self(), key);
    }

    public T requestDescription(String operationKey, String requestDescription) {
        require(operationKey, "operation key");
        require(requestDescription, "request description");
        contracts().operationRequestDescription(operationKey, requestDescription);
        return self();
    }

    public T onChannelEvent(String workflowKey,
                            String channelKey,
                            Class<?> eventTypeClass,
                            Consumer<StepsBuilder> customizer) {
        require(workflowKey, "workflow key");
        require(channelKey, "channel key");
        require(eventTypeClass, "event type");
        require(customizer, "steps");
        contracts().onEvent(workflowKey, channelKey, eventTypeClass, customizer);
        return self();
    }

    public T onEvent(String workflowKey,
                     Class<?> eventTypeClass,
                     Consumer<StepsBuilder> customizer) {
        require(workflowKey, "workflow key");
        require(eventTypeClass, "event type");
        require(customizer, "steps");
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, eventTypeClass, customizer);
        return self();
    }

    public T onDocChange(String workflowKey, String path, Consumer<StepsBuilder> customizer) {
        require(workflowKey, "workflow key");
        require(path, "path");
        require(customizer, "steps");
        String channelKey = workflowKey + "DocUpdateChannel";
        ContractsBuilder contracts = contracts();
        contracts.documentUpdateChannel(channelKey, path);
        contracts.sequentialWorkflow(
                workflowKey,
                channelKey,
                new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE),
                customizer);
        return self();
    }

    public T onInit(String workflowKey, Consumer<StepsBuilder> customizer) {
        require(workflowKey, "workflow key");
        require(customizer, "steps");
        ensureInitChannel();
        contracts().onLifecycle(workflowKey, "initLifecycleChannel", customizer);
        return self();
    }

    public T myOsAdmin(String channelKey) {
        require(channelKey, "channel key");
        channel(channelKey);
        String operationKey = deriveAdminOperationKey(channelKey);
        operation(operationKey, channelKey, "Accept events from MyOS admin", steps ->
                steps.jsRaw("EmitAdminEvents", "return { events: event?.message?.request ?? [] };"));
        return self();
    }

    public T onMyOsResponse(String workflowKey,
                            Class<?> responseEventTypeClass,
                            String requestId,
                            Consumer<StepsBuilder> customizer) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return onTriggeredWithMatcher(workflowKey, responseEventTypeClass, null, customizer);
        }
        return onTriggeredWithId(workflowKey, responseEventTypeClass, "requestId", requestId, customizer);
    }

    public T onMyOsResponse(String workflowKey,
                            Class<?> responseEventTypeClass,
                            Consumer<StepsBuilder> customizer) {
        return onMyOsResponse(workflowKey, responseEventTypeClass, null, customizer);
    }

    public T directChange(String operationName,
                          String channelKey,
                          String description) {
        operation(operationName, channelKey, description, steps -> steps
                .jsRaw(
                        "CollectChangeset",
                        "const request = event?.message?.request ?? {}; return { events: [], changeset: request.changeset ?? [] };")
                .updateDocumentFromExpression("ApplyChangeset", "steps.CollectChangeset.changeset"));
        policies().contractsChangePolicy("direct-change", "operation applies request changeset");
        return self();
    }

    public T onTriggeredWithId(String workflowKey,
                               Class<?> eventClass,
                               String idFieldName,
                               String idValue,
                               Consumer<StepsBuilder> customizer) {
        require(idFieldName, "id field name");
        require(idValue, "id value");

        TriggeredIdMatcher matcher = new TriggeredIdMatcher();
        String normalizedField = idFieldName.trim();
        String normalizedValue = idValue.trim();
        if ("requestId".equals(normalizedField)) {
            matcher.requestId = normalizedValue;
            matcher.inResponseTo = new CorrelationRef();
            matcher.inResponseTo.requestId = normalizedValue;
        } else if ("subscriptionId".equals(normalizedField)) {
            matcher.subscriptionId = normalizedValue;
        } else {
            throw new IllegalArgumentException("Unsupported id field for matcher: " + normalizedField);
        }
        return onTriggeredWithMatcher(workflowKey, eventClass, matcher, customizer);
    }

    public T onTriggeredWithMatcher(String workflowKey,
                                    Class<?> eventClass,
                                    Object matcherBean,
                                    Consumer<StepsBuilder> customizer) {
        require(workflowKey, "workflow key");
        require(eventClass, "event class");
        require(customizer, "steps");
        ensureTriggeredChannel();

        Node matcher = matcherBean == null ? typeOnlyNode(eventClass) : BLUE.objectToNode(matcherBean);
        matcher.type(TypeRef.of(eventClass).alias());
        contracts().onTriggered(workflowKey, matcher, customizer);
        return self();
    }

    public T onSubscriptionUpdate(String workflowKey,
                                  String subscriptionId,
                                  Class<?> updateTypeClass,
                                  Consumer<StepsBuilder> customizer) {
        require(subscriptionId, "subscription id");

        SubscriptionUpdateMatcher matcher = new SubscriptionUpdateMatcher();
        matcher.subscriptionId = subscriptionId.trim();
        if (updateTypeClass != null) {
            matcher.update = typeOnlyNode(updateTypeClass);
        }
        return onTriggeredWithMatcher(
                workflowKey,
                SubscriptionUpdate.class,
                matcher,
                customizer);
    }

    public T onSubscriptionUpdate(String workflowKey,
                                  String subscriptionId,
                                  Consumer<StepsBuilder> customizer) {
        return onSubscriptionUpdate(workflowKey, subscriptionId, null, customizer);
    }

    public T set(String pointer, Object value) {
        setPointer(pointer, toNode(value));
        addFieldToActiveSection(pointer);
        return self();
    }

    public T field(String pointer, Object value) {
        return set(pointer, value);
    }

    public T field(String pointer) {
        require(pointer, "pointer");
        addFieldToActiveSection(pointer);
        return self();
    }

    public T replace(String pointer, Object value) {
        setPointer(pointer, toNode(value));
        addFieldToActiveSection(pointer);
        return self();
    }

    public T remove(String pointer) {
        removePointer(pointer);
        return self();
    }

    public Node buildDocument() {
        return document;
    }

    public static String expr(String expression) {
        if (expression == null) {
            return null;
        }
        String trimmed = expression.trim();
        return trimmed.startsWith("${") ? trimmed : "${" + trimmed + "}";
    }

    private ContractsBuilder contracts() {
        return new ContractsBuilder(ensureMap(document, "contracts"));
    }

    private PoliciesBuilder policies() {
        return new PoliciesBuilder(ensureMap(document, "policies"));
    }

    private static Node toNode(Object value) {
        if (value instanceof Node) {
            return (Node) value;
        }
        return new Node().value(value);
    }

    private void ensureTriggeredChannel() {
        Map<String, Node> contracts = ensureMap(document, "contracts");
        if (!contracts.containsKey("triggeredEventChannel")) {
            new ContractsBuilder(contracts).triggeredEventChannel("triggeredEventChannel");
        }
    }

    private void ensureInitChannel() {
        Map<String, Node> contracts = ensureMap(document, "contracts");
        if (!contracts.containsKey("initLifecycleChannel")) {
            new ContractsBuilder(contracts).lifecycleEventChannel(
                    "initLifecycleChannel",
                    TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
        }
    }

    private static Map<String, Node> ensureProperties(Node node) {
        if (node.getProperties() == null) {
            node.properties(new LinkedHashMap<String, Node>());
        }
        return node.getProperties();
    }

    private static Map<String, Node> ensureMap(Node parent, String key) {
        Map<String, Node> props = ensureProperties(parent);
        Node child = props.get(key);
        if (child == null) {
            child = new Node().properties(new LinkedHashMap<String, Node>());
            props.put(key, child);
        } else if (child.getProperties() == null) {
            child.properties(new LinkedHashMap<String, Node>());
        }
        return child.getProperties();
    }

    private Node ensureSectionContract(String sectionKey) {
        Map<String, Node> contracts = ensureMap(document, "contracts");
        Node section = contracts.get(sectionKey);
        if (section == null) {
            section = new Node().type("Conversation/Document Section");
            section.properties("title", new Node().value(sectionKey));
            section.properties("summary", new Node().value("Auto-generated section"));
            contracts.put(sectionKey, section);
        } else if (section.getType() == null) {
            section.type("Conversation/Document Section");
        }
        return section;
    }

    private static Node ensureSectionListNode(Node section, String key) {
        if (section.getProperties() == null) {
            section.properties(new LinkedHashMap<String, Node>());
        }
        Node listNode = section.getProperties().get(key);
        if (listNode == null) {
            listNode = new Node().items(new ArrayList<Node>());
            section.getProperties().put(key, listNode);
            return listNode;
        }
        if (listNode.getItems() == null) {
            listNode.items(new ArrayList<Node>());
        }
        return listNode;
    }

    private void addFieldToActiveSection(String pointer) {
        if (activeSectionKey == null || pointer == null || pointer.isBlank()) {
            return;
        }
        Node section = ensureSectionContract(activeSectionKey);
        Node relatedFields = ensureSectionListNode(section, "relatedFields");
        addStringToListIfMissing(relatedFields, pointer.trim());
    }

    private void addContractToActiveSection(String contractKey) {
        if (activeSectionKey == null || contractKey == null || contractKey.isBlank()) {
            return;
        }
        Node section = ensureSectionContract(activeSectionKey);
        Node relatedContracts = ensureSectionListNode(section, "relatedContracts");
        addStringToListIfMissing(relatedContracts, contractKey.trim());
    }

    private static void addStringToListIfMissing(Node listNode, String value) {
        if (listNode == null || listNode.getItems() == null) {
            return;
        }
        for (Node item : listNode.getItems()) {
            if (item != null && value.equals(item.getValue())) {
                return;
            }
        }
        listNode.getItems().add(new Node().value(value));
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
        assign(current, leaf, valueNode, normalized);
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

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static String deriveAdminOperationKey(String channelKey) {
        String trimmed = channelKey.trim();
        if (trimmed.endsWith("Channel") && trimmed.length() > "Channel".length()) {
            return trimmed.substring(0, trimmed.length() - "Channel".length()) + "Update";
        }
        return trimmed + "Update";
    }

    private static Node typeOnlyNode(Class<?> typeClass) {
        try {
            Object instance = typeClass.getDeclaredConstructor().newInstance();
            return BLUE.objectToNode(instance);
        } catch (Exception ignored) {
            return new Node().type(TypeRef.of(typeClass).asTypeNode());
        }
    }

    private static void pruneEmptyEventProperty(Node channelNode) {
        if (channelNode == null || channelNode.getProperties() == null) {
            return;
        }
        Node event = channelNode.getProperties().get("event");
        if (event == null) {
            return;
        }
        boolean emptyObject = event.getValue() == null
                && event.getType() == null
                && event.getBlueId() == null
                && event.getItems() == null
                && (event.getProperties() == null || event.getProperties().isEmpty());
        if (emptyObject) {
            channelNode.getProperties().remove("event");
        }
    }

    private static final class TriggeredIdMatcher {
        public String requestId;
        public String subscriptionId;
        public CorrelationRef inResponseTo;
    }

    private static final class CorrelationRef {
        public String requestId;
    }

    private static final class SubscriptionUpdateMatcher {
        public String subscriptionId;
        public Node update;
    }

    public static final class OperationBuilder<P extends DocBuilder<P>> {
        private final P parent;
        private final String key;
        private String channelKey;
        private String description;
        private Class<?> requestTypeClass;
        private String requestDescription;
        private Consumer<StepsBuilder> implementation;

        private OperationBuilder(P parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        public OperationBuilder<P> channel(String channelKey) {
            this.channelKey = channelKey;
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

        public OperationBuilder<P> requestDescription(String requestDescription) {
            require(requestDescription, "request description");
            this.requestDescription = requestDescription;
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
            require(channelKey, "channel");
            require(implementation, "steps");
            P result = parent.operation(key, channelKey, requestTypeClass, description, implementation);
            if (requestDescription != null) {
                result.requestDescription(key, requestDescription);
            }
            return result;
        }
    }
}
