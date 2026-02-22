package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DocumentBuilder {

    private final Node root = new Node();
    private final Node document = new Node();
    private final Map<String, Node> contracts = new LinkedHashMap<String, Node>();
    private final Map<String, Node> policies = new LinkedHashMap<String, Node>();
    private final Map<String, Node> channelBindings = new LinkedHashMap<String, Node>();

    private DocumentBuilder(String typeAlias) {
        root.type(typeAlias);
    }

    public static DocumentBuilder documentSessionBootstrap() {
        return new DocumentBuilder(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP);
    }

    public DocumentBuilder documentName(String name) {
        document.name(name);
        return this;
    }

    public DocumentBuilder documentType(String typeAlias) {
        document.type(typeAlias);
        return this;
    }

    public DocumentBuilder documentType(Class<?> typeClass) {
        document.type(TypeRef.of(typeClass).asTypeNode());
        return this;
    }

    public DocumentBuilder documentDescription(String description) {
        document.description(description);
        return this;
    }

    public DocumentBuilder putDocumentValue(String key, Object value) {
        if (value instanceof Node) {
            document.properties(key, (Node) value);
        } else {
            document.properties(key, new Node().value(value));
        }
        return this;
    }

    public DocumentBuilder putDocumentExpression(String key, String expression) {
        document.properties(key, new Node().value(BlueDocDsl.expr(expression)));
        return this;
    }

    public DocumentBuilder putDocumentObject(String key, Consumer<NodeObjectBuilder> customizer) {
        NodeObjectBuilder objectBuilder = NodeObjectBuilder.create();
        customizer.accept(objectBuilder);
        document.properties(key, objectBuilder.build());
        return this;
    }

    public DocumentBuilder contracts(Consumer<ContractsBuilder> customizer) {
        ContractsBuilder contractsBuilder = new ContractsBuilder(contracts);
        customizer.accept(contractsBuilder);
        return this;
    }

    public DocumentBuilder policies(Consumer<PoliciesBuilder> customizer) {
        PoliciesBuilder policiesBuilder = new PoliciesBuilder(policies);
        customizer.accept(policiesBuilder);
        return this;
    }

    public DocumentBuilder bindAccount(String channelKey, String accountId) {
        Node binding = channelBindings.get(channelKey);
        if (binding == null) {
            binding = new Node();
            channelBindings.put(channelKey, binding);
        }
        binding.properties("accountId", new Node().value(accountId));
        return this;
    }

    public DocumentBuilder bindEmail(String channelKey, String email) {
        Node binding = channelBindings.get(channelKey);
        if (binding == null) {
            binding = new Node();
            channelBindings.put(channelKey, binding);
        }
        binding.properties("email", new Node().value(email));
        return this;
    }

    public Node build() {
        if (!contracts.isEmpty()) {
            document.properties("contracts", new Node().properties(contracts));
        }
        if (!policies.isEmpty()) {
            document.properties("policies", new Node().properties(policies));
        }
        root.properties("document", document);
        if (!channelBindings.isEmpty()) {
            root.properties("channelBindings", new Node().properties(channelBindings));
        }
        return root;
    }
}
