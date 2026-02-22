package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

public final class NodeObjectBuilder {

    private final Node node = new Node();

    private NodeObjectBuilder() {
    }

    public static NodeObjectBuilder create() {
        return new NodeObjectBuilder();
    }

    public NodeObjectBuilder type(String typeAlias) {
        node.type(typeAlias);
        return this;
    }

    public NodeObjectBuilder type(Class<?> typeClass) {
        node.type(TypeRef.of(typeClass).asTypeNode());
        return this;
    }

    public NodeObjectBuilder put(String key, Object value) {
        if (value instanceof Node) {
            node.properties(key, (Node) value);
            return this;
        }
        node.properties(key, new Node().value(value));
        return this;
    }

    public NodeObjectBuilder putNode(String key, Node value) {
        node.properties(key, value);
        return this;
    }

    public NodeObjectBuilder putExpression(String key, String expression) {
        node.properties(key, new Node().value(BlueDocDsl.expr(expression)));
        return this;
    }

    public Node build() {
        return node;
    }
}
