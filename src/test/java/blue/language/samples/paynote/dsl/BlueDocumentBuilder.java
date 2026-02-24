package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class BlueDocumentBuilder {

    private final Node document = new Node();
    private final Map<String, Node> contracts = new LinkedHashMap<String, Node>();
    private final Map<String, Node> policies = new LinkedHashMap<String, Node>();

    private BlueDocumentBuilder(Class<?> typeClass) {
        document.type(TypeRef.of(typeClass).asTypeNode());
    }

    public static BlueDocumentBuilder document(Class<?> typeClass) {
        return new BlueDocumentBuilder(typeClass);
    }

    public BlueDocumentBuilder name(String name) {
        document.name(name);
        return this;
    }

    public BlueDocumentBuilder description(String description) {
        document.description(description);
        return this;
    }

    public BlueDocumentBuilder putValue(String key, Object value) {
        document.properties(key, new Node().value(value));
        return this;
    }

    public BlueDocumentBuilder putExpression(String key, String expression) {
        document.properties(key, new Node().value(BlueDocDsl.expr(expression)));
        return this;
    }

    public BlueDocumentBuilder putNode(String key, Node value) {
        document.properties(key, value);
        return this;
    }

    public BlueDocumentBuilder contracts(Consumer<ContractsBuilder> customizer) {
        ContractsBuilder contractsBuilder = new ContractsBuilder(contracts);
        customizer.accept(contractsBuilder);
        return this;
    }

    public BlueDocumentBuilder policies(Consumer<PoliciesBuilder> customizer) {
        PoliciesBuilder policiesBuilder = new PoliciesBuilder(policies);
        customizer.accept(policiesBuilder);
        return this;
    }

    public Node build() {
        if (!contracts.isEmpty()) {
            document.properties("contracts", new Node().properties(contracts));
        }
        if (!policies.isEmpty()) {
            document.properties("policies", new Node().properties(policies));
        }
        return document;
    }
}
