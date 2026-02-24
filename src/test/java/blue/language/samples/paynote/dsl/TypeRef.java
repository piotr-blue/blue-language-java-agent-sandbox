package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

public final class TypeRef {

    private final String alias;
    private final String blueId;

    private TypeRef(String alias, String blueId) {
        this.alias = alias;
        this.blueId = blueId;
    }

    public static TypeRef alias(String alias) {
        return new TypeRef(alias, null);
    }

    public static TypeRef of(Class<?> typeClass) {
        if (typeClass == null) {
            throw new IllegalArgumentException("typeClass cannot be null");
        }
        TypeAlias aliasAnnotation = typeClass.getAnnotation(TypeAlias.class);
        String alias = aliasAnnotation != null
                ? aliasAnnotation.value()
                : TypeAliases.aliasForClass(typeClass);

        TypeBlueId blueIdAnnotation = typeClass.getAnnotation(TypeBlueId.class);
        String blueId = null;
        if (blueIdAnnotation != null) {
            String[] values = blueIdAnnotation.value();
            if (values != null && values.length > 0 && values[0] != null && !values[0].trim().isEmpty()) {
                blueId = values[0];
            } else if (blueIdAnnotation.defaultValue() != null && !blueIdAnnotation.defaultValue().trim().isEmpty()) {
                blueId = blueIdAnnotation.defaultValue().trim();
            }
        }
        return new TypeRef(alias, blueId);
    }

    public String alias() {
        return alias;
    }

    public String blueId() {
        return blueId;
    }

    public Node asTypeNode() {
        Node typeNode = new Node();
        if (alias != null) {
            typeNode.value(alias);
        }
        if (blueId != null) {
            typeNode.blueId(blueId);
        }
        return typeNode.inlineValue(true);
    }
}
