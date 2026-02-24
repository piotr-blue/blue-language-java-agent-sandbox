package blue.language.samples.paynote.dsl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsTemplateResolver {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Z0-9_]+)\\s*}}");
    private static final Map<String, String> DEFAULT_TOKENS = buildDefaultTokens();

    private JsTemplateResolver() {
    }

    public static String resolveDefaults(String template) {
        return resolve(template, DEFAULT_TOKENS);
    }

    public static String resolve(String template, Map<String, String> tokens) {
        if (template == null) {
            return null;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            String value = tokens != null ? tokens.get(token) : null;
            if (value == null) {
                throw new IllegalArgumentException("Unknown JS template token: " + token);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static Map<String, String> defaultTokens() {
        return DEFAULT_TOKENS;
    }

    private static Map<String, String> buildDefaultTokens() {
        Map<String, String> tokens = new LinkedHashMap<String, String>();
        registerConstants(tokens, TypeAliases.class);
        registerConstants(tokens, PayNoteAliases.class);
        return Collections.unmodifiableMap(tokens);
    }

    private static void registerConstants(Map<String, String> target, Class<?> constantsClass) {
        Field[] fields = constantsClass.getFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                continue;
            }
            if (!String.class.equals(field.getType())) {
                continue;
            }
            try {
                Object rawValue = field.get(null);
                if (rawValue instanceof String) {
                    target.put(field.getName(), (String) rawValue);
                }
            } catch (IllegalAccessException ignored) {
                // Public fields should be accessible; skip if inaccessible.
            }
        }
    }
}
