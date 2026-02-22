package blue.language.samples.paynote.dsl;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsObjectBuilder {

    private final Map<String, String> properties = new LinkedHashMap<String, String>();

    private JsObjectBuilder() {
    }

    public static JsObjectBuilder object() {
        return new JsObjectBuilder();
    }

    public JsObjectBuilder propRaw(String key, String rawValue) {
        properties.put(key, rawValue);
        return this;
    }

    public JsObjectBuilder propString(String key, String value) {
        properties.put(key, "'" + escapeString(value) + "'");
        return this;
    }

    public JsObjectBuilder propNumber(String key, Number value) {
        properties.put(key, String.valueOf(value));
        return this;
    }

    public JsObjectBuilder propBoolean(String key, boolean value) {
        properties.put(key, String.valueOf(value));
        return this;
    }

    public JsObjectBuilder propObject(String key, JsObjectBuilder nested) {
        properties.put(key, nested.build());
        return this;
    }

    public JsObjectBuilder propArrayRaw(String key, String rawArray) {
        properties.put(key, rawArray);
        return this;
    }

    public JsObjectBuilder propArray(String key, JsArrayBuilder array) {
        properties.put(key, array.build());
        return this;
    }

    public String build() {
        StringBuilder out = new StringBuilder();
        out.append("{ ");
        int index = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (index > 0) {
                out.append(", ");
            }
            out.append(entry.getKey()).append(": ").append(entry.getValue());
            index++;
        }
        out.append(" }");
        return out.toString();
    }

    private String escapeString(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }
}
