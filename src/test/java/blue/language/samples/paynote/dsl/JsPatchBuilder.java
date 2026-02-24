package blue.language.samples.paynote.dsl;

public final class JsPatchBuilder {

    private final JsArrayBuilder entries = JsArrayBuilder.array();

    private JsPatchBuilder() {
    }

    public static JsPatchBuilder patch() {
        return new JsPatchBuilder();
    }

    public static JsPatchBuilder changeset() {
        return patch();
    }

    public JsPatchBuilder replaceValue(String path, String rawValueExpression) {
        entries.itemObject(JsObjectBuilder.object()
                .propString("op", "replace")
                .propString("path", path)
                .propRaw("val", rawValueExpression));
        return this;
    }

    public JsPatchBuilder replaceExpression(String path, String rawExpression) {
        return replaceValue(path, rawExpression);
    }

    public JsPatchBuilder addValue(String path, String rawValueExpression) {
        entries.itemObject(JsObjectBuilder.object()
                .propString("op", "add")
                .propString("path", path)
                .propRaw("val", rawValueExpression));
        return this;
    }

    public JsPatchBuilder removePath(String path) {
        entries.itemObject(JsObjectBuilder.object()
                .propString("op", "remove")
                .propString("path", path));
        return this;
    }

    public String build() {
        return entries.build();
    }
}
