package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.function.Consumer;

public final class BlueDocDsl {

    private BlueDocDsl() {
    }

    public static DocumentBuilder documentSessionBootstrap() {
        return DocumentBuilder.documentSessionBootstrap();
    }

    public static BlueDocumentBuilder document(Class<?> documentTypeClass) {
        return BlueDocumentBuilder.document(documentTypeClass);
    }

    public static BlueDocumentBuilder overlay(Class<?> documentTypeClass) {
        return BlueDocumentBuilder.document(documentTypeClass);
    }

    public static NodeObjectBuilder object() {
        return NodeObjectBuilder.create();
    }

    public static Node typed(String typeAlias) {
        return new Node().type(typeAlias);
    }

    public static Node typed(Class<?> typeClass) {
        return new Node().type(TypeRef.of(typeClass).asTypeNode());
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

    public static JsProgram js(Consumer<JsProgram.Builder> customizer) {
        JsProgram.Builder builder = JsProgram.builder();
        customizer.accept(builder);
        return builder.build();
    }
}
