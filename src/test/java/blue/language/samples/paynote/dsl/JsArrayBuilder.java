package blue.language.samples.paynote.dsl;

import java.util.ArrayList;
import java.util.List;

public final class JsArrayBuilder {

    private final List<String> values = new ArrayList<String>();

    private JsArrayBuilder() {
    }

    public static JsArrayBuilder array() {
        return new JsArrayBuilder();
    }

    public JsArrayBuilder itemRaw(String rawValue) {
        values.add(rawValue);
        return this;
    }

    public JsArrayBuilder itemString(String value) {
        values.add("'" + escape(value) + "'");
        return this;
    }

    public JsArrayBuilder itemObject(JsObjectBuilder object) {
        values.add(object.build());
        return this;
    }

    public JsArrayBuilder itemTypedEvent(Class<?> eventTypeClass) {
        return itemObject(JsCommon.typedEvent(eventTypeClass, null));
    }

    public String build() {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(values.get(i));
        }
        out.append("]");
        return out.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
