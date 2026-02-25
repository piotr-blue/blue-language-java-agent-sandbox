package blue.language.samples.paynote.sdk;

public final class SimpleDocBuilder extends DocBuilder<SimpleDocBuilder> {

    private SimpleDocBuilder() {
        super();
    }

    private SimpleDocBuilder(blue.language.model.Node existingDocument) {
        super(existingDocument);
    }

    public static SimpleDocBuilder doc() {
        return new SimpleDocBuilder();
    }

    public static SimpleDocBuilder name(String name) {
        return doc().withName(name);
    }

    public static SimpleDocBuilder edit(blue.language.model.Node existingDocument) {
        return new SimpleDocBuilder(existingDocument);
    }

    public static SimpleDocBuilder from(blue.language.model.Node existingDocument) {
        return edit(existingDocument);
    }

    public static String expr(String expression) {
        return DocBuilder.expr(expression);
    }
}
