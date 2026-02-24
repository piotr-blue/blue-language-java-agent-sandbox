package blue.language.samples.paynote.dsl;

public final class JsOutputBuilder {

    private final JsObjectBuilder out = JsObjectBuilder.object();

    private JsOutputBuilder() {
    }

    public static JsOutputBuilder output() {
        return new JsOutputBuilder();
    }

    public JsOutputBuilder changesetRaw(String changesetExpression) {
        out.propRaw("changeset", changesetExpression);
        return this;
    }

    public JsOutputBuilder eventsRaw(String eventsExpression) {
        out.propRaw("events", eventsExpression);
        return this;
    }

    public JsOutputBuilder eventsArray(JsArrayBuilder eventsArray) {
        out.propRaw("events", eventsArray.build());
        return this;
    }

    public JsOutputBuilder emptyEvents() {
        out.propRaw("events", "[]");
        return this;
    }

    public String build() {
        return out.build();
    }
}
