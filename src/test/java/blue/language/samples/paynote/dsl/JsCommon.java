package blue.language.samples.paynote.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class JsCommon {

    private JsCommon() {
    }

    public static String readRequest() {
        return "event.message?.request ?? {}";
    }

    public static String coalesce(String... expressions) {
        List<String> parts = new ArrayList<String>();
        if (expressions != null) {
            for (String expression : expressions) {
                if (expression == null) {
                    continue;
                }
                String trimmed = expression.trim();
                if (!trimmed.isEmpty()) {
                    parts.add(trimmed);
                }
            }
        }
        if (parts.isEmpty()) {
            return "undefined";
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                joined.append(" ?? ");
            }
            joined.append(parts.get(i));
        }
        return joined.toString();
    }

    public static String safeNumber(String rawExpression, String fallbackExpression) {
        String raw = rawExpression == null || rawExpression.trim().isEmpty() ? "undefined" : rawExpression.trim();
        String fallback = fallbackExpression == null || fallbackExpression.trim().isEmpty()
                ? "0"
                : fallbackExpression.trim();
        return "(function(value){ const n = Number(value); return Number.isFinite(n) ? n : " + fallback
                + "; })(" + raw + ")";
    }

    public static JsObjectBuilder typedEvent(Class<?> eventTypeClass, Consumer<JsObjectBuilder> payloadCustomizer) {
        JsObjectBuilder event = JsObjectBuilder.object()
                .propString("type", TypeRef.of(eventTypeClass).alias());
        if (payloadCustomizer != null) {
            payloadCustomizer.accept(event);
        }
        return event;
    }
}
