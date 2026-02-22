package blue.language.processor.script;

import blue.language.model.Node;
import blue.language.processor.ProcessorGasSchedule;
import blue.language.processor.ProcessorExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

public final class QuickJsExpressionUtils {

    private static final Pattern STANDALONE_EXPRESSION_PATTERN = Pattern.compile("^\\$\\{([\\s\\S]*)}$");
    private static final Pattern SINGLE_EXPRESSION_PATTERN = Pattern.compile("\\$\\{([\\s\\S]+?)}");
    private static final Pattern TEMPLATE_EXPRESSION_PATTERN = Pattern.compile("\\$\\{([\\s\\S]+?)}");

    public interface PointerPredicate {
        boolean test(String pointer, Node node);
    }

    public static final class PathMatchOptions {
        private final boolean dot;
        private final boolean nocase;
        private final boolean noglobstar;

        public PathMatchOptions() {
            this(true, false, false);
        }

        public PathMatchOptions(boolean dot, boolean nocase, boolean noglobstar) {
            this.dot = dot;
            this.nocase = nocase;
            this.noglobstar = noglobstar;
        }

        public boolean dot() {
            return dot;
        }

        public boolean nocase() {
            return nocase;
        }

        public boolean noglobstar() {
            return noglobstar;
        }
    }

    private QuickJsExpressionUtils() {
    }

    public static boolean isExpression(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String text = (String) value;
        if (!STANDALONE_EXPRESSION_PATTERN.matcher(text).matches()) {
            return false;
        }
        int first = text.indexOf("${");
        int last = text.lastIndexOf("${");
        return first >= 0 && first == last;
    }

    public static boolean containsExpression(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String text = (String) value;
        if (STANDALONE_EXPRESSION_PATTERN.matcher(text).matches()) {
            return true;
        }
        return SINGLE_EXPRESSION_PATTERN.matcher(text).find();
    }

    public static String extractExpressionContent(String expression) {
        if (!isExpression(expression)) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        String text = expression.trim();
        return text.substring(2, text.length() - 1);
    }

    public static PointerPredicate createPathPredicate(List<String> includePatterns, List<String> excludePatterns) {
        return createPathPredicate(includePatterns, excludePatterns, new PathMatchOptions());
    }

    public static PointerPredicate createPathPredicate(List<String> includePatterns,
                                                       List<String> excludePatterns,
                                                       PathMatchOptions options) {
        final List<String> includes = includePatterns == null || includePatterns.isEmpty()
                ? Collections.singletonList("/**")
                : includePatterns;
        final List<String> excludes = excludePatterns == null ? Collections.<String>emptyList() : excludePatterns;
        final PathMatchOptions effectiveOptions = options == null ? new PathMatchOptions() : options;
        return new PointerPredicate() {
            @Override
            public boolean test(String pointer, Node node) {
                String normalized = normalizePointer(pointer);
                boolean included = false;
                for (String pattern : includes) {
                    if (matchesPattern(normalized, pattern, effectiveOptions)) {
                        included = true;
                        break;
                    }
                }
                if (!included) {
                    return false;
                }
                for (String pattern : excludes) {
                    if (matchesPattern(normalized, pattern, effectiveOptions)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public static Node resolveExpressions(Node node,
                                          QuickJSEvaluator evaluator,
                                          Map<String, Object> bindings,
                                          ProcessorExecutionContext context,
                                          PointerPredicate shouldResolve,
                                          PointerPredicate shouldDescend) {
        return resolveExpressions(node, evaluator, bindings, context, shouldResolve, shouldDescend, new Consumer<java.math.BigInteger>() {
            @Override
            public void accept(java.math.BigInteger amount) {
                context.chargeWasmGas(amount);
            }
        });
    }

    public static Node resolveExpressions(Node node,
                                          QuickJSEvaluator evaluator,
                                          Map<String, Object> bindings,
                                          ProcessorExecutionContext context,
                                          PointerPredicate shouldResolve,
                                          PointerPredicate shouldDescend,
                                          Consumer<java.math.BigInteger> wasmGasConsumer) {
        if (node == null) {
            return null;
        }
        return resolveRecursive(node, "/", evaluator, bindings, context, shouldResolve, shouldDescend, wasmGasConsumer);
    }

    private static Node resolveRecursive(Node node,
                                         String pointer,
                                         QuickJSEvaluator evaluator,
                                         Map<String, Object> bindings,
                                         ProcessorExecutionContext context,
                                         PointerPredicate shouldResolve,
                                         PointerPredicate shouldDescend,
                                         Consumer<java.math.BigInteger> wasmGasConsumer) {
        Node cloned = node.clone();
        if (shouldDescend != null && !shouldDescend.test(pointer, cloned)) {
            return cloned;
        }
        Object value = cloned.getValue();
        if (shouldResolve == null || shouldResolve.test(pointer, cloned)) {
            if (isExpression(value)) {
                Object evaluated = evaluateQuickJsExpression(
                        evaluator,
                        extractExpressionContent(String.valueOf(value)),
                        bindings,
                        wasmGasConsumer);
                return toNode(evaluated);
            }
            if (containsExpression(value)) {
                String rendered = resolveTemplateString(
                        evaluator,
                        String.valueOf(value),
                        bindings,
                        wasmGasConsumer);
                return new Node().value(rendered);
            }
        }

        if (cloned.getProperties() != null) {
            for (Map.Entry<String, Node> entry : cloned.getProperties().entrySet()) {
                String childPointer = appendPointerSegment(pointer, escapeSegment(entry.getKey()));
                if (shouldDescend != null && !shouldDescend.test(childPointer, entry.getValue())) {
                    continue;
                }
                entry.setValue(resolveRecursive(entry.getValue(), childPointer, evaluator, bindings, context, shouldResolve, shouldDescend, wasmGasConsumer));
            }
        }
        if (cloned.getItems() != null) {
            List<Node> updatedItems = new ArrayList<>();
            List<Node> items = cloned.getItems();
            for (int i = 0; i < items.size(); i++) {
                Node child = items.get(i);
                String childPointer = appendPointerSegment(pointer, String.valueOf(i));
                if (shouldDescend != null && !shouldDescend.test(childPointer, child)) {
                    updatedItems.add(child);
                    continue;
                }
                updatedItems.add(resolveRecursive(child, childPointer, evaluator, bindings, context, shouldResolve, shouldDescend, wasmGasConsumer));
            }
            cloned.items(updatedItems);
        }
        return cloned;
    }

    public static Object evaluateQuickJsExpression(QuickJSEvaluator evaluator,
                                                   String code,
                                                   Map<String, Object> bindings,
                                                   Consumer<java.math.BigInteger> wasmGasConsumer) {
        try {
            ScriptRuntimeResult runtimeResult = evaluator.evaluate(
                    code,
                    bindings,
                    ProcessorGasSchedule.DEFAULT_EXPRESSION_WASM_GAS_LIMIT);
            if (wasmGasConsumer != null && runtimeResult.wasmGasUsed() != null) {
                wasmGasConsumer.accept(runtimeResult.wasmGasUsed());
            }
            return runtimeResult.value();
        } catch (CodeBlockEvaluationError ex) {
            throw new CodeBlockEvaluationError(code, ex);
        }
    }

    public static String resolveTemplateString(QuickJSEvaluator evaluator,
                                               String template,
                                               Map<String, Object> bindings,
                                               Consumer<java.math.BigInteger> wasmGasConsumer) {
        Matcher matcher = TEMPLATE_EXPRESSION_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value = evaluateQuickJsExpression(evaluator, expression, bindings, wasmGasConsumer);
            String replacement = value == null ? "" : String.valueOf(value);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static Node toNode(Object value) {
        if (value instanceof Node) {
            return ((Node) value).clone();
        }
        if (value instanceof Map) {
            Node node = new Node();
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    node.properties(String.valueOf(entry.getKey()), toNode(entry.getValue()));
                }
            }
            return node;
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            List<Node> items = new ArrayList<>();
            for (Object element : list) {
                items.add(toNode(element));
            }
            return new Node().items(items);
        }
        return new Node().value(value);
    }

    private static String escapeSegment(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static String appendPointerSegment(String pointer, String segment) {
        String normalized = normalizePointer(pointer);
        if ("/".equals(normalized)) {
            return "/" + segment;
        }
        return normalized + "/" + segment;
    }

    private static boolean matchesPattern(String pointer, String pattern, PathMatchOptions options) {
        String normalizedPattern = normalizePointer(pattern);
        String pointerToMatch = pointer;
        if (options.nocase()) {
            normalizedPattern = normalizedPattern.toLowerCase(java.util.Locale.ROOT);
            pointerToMatch = pointerToMatch.toLowerCase(java.util.Locale.ROOT);
        }
        List<String> expandedPatterns = new ArrayList<>();
        expandedPatterns.addAll(expandBraces(normalizedPattern));

        for (String expandedPattern : expandedPatterns) {
            String effectivePattern = options.noglobstar()
                    ? expandedPattern.replace("**", "*")
                    : expandedPattern;
            if (matchesSinglePattern(pointerToMatch, effectivePattern, options.dot())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesSinglePattern(String pointerToMatch, String normalizedPattern, boolean dot) {
        String regexPattern = buildPatternRegex(normalizedPattern, dot, true);
        boolean matched = pointerToMatch.matches(regexPattern);
        if (!matched) {
            return false;
        }
        if (!dot
                && pointerToMatch.matches(".*(^|/)\\.[^/]+(/|$).*")
                && !normalizedPattern.contains("/.")) {
            return false;
        }
        return true;
    }

    private static String buildPatternRegex(String normalizedPattern, boolean dot, boolean anchored) {
        StringBuilder regex = new StringBuilder();
        if (anchored) {
            regex.append("^");
        }
        for (int i = 0; i < normalizedPattern.length(); i++) {
            char ch = normalizedPattern.charAt(i);
            boolean segmentStart = i == 0 || normalizedPattern.charAt(i - 1) == '/';
            if (isExtglobMarker(ch) && i + 1 < normalizedPattern.length() && normalizedPattern.charAt(i + 1) == '(') {
                int closingParenthesis = findClosingParenthesis(normalizedPattern, i + 1);
                if (closingParenthesis > i + 1) {
                    String body = normalizedPattern.substring(i + 2, closingParenthesis);
                    if (segmentStart && !dot) {
                        regex.append("(?!\\.)");
                    }
                    regex.append(toExtglobRegex(ch, body, dot));
                    i = closingParenthesis;
                    continue;
                }
            }
            if (ch == '*') {
                boolean isDoubleStar = (i + 1 < normalizedPattern.length() && normalizedPattern.charAt(i + 1) == '*');
                if (isDoubleStar) {
                    regex.append(".*");
                    i++;
                    continue;
                }
                regex.append(segmentStart && !dot ? "(?!\\.)[^/]*" : "[^/]*");
                continue;
            }
            if (ch == '?') {
                regex.append(segmentStart && !dot ? "(?!\\.)[^/]" : "[^/]");
                continue;
            }
            if (ch == '[') {
                int closingBracket = findClosingBracket(normalizedPattern, i + 1);
                if (closingBracket > i + 1) {
                    if (segmentStart && !dot) {
                        regex.append("(?!\\.)");
                    }
                    regex.append(toCharacterClassRegex(
                            normalizedPattern.substring(i + 1, closingBracket)));
                    i = closingBracket;
                    continue;
                }
            }
            if ("\\.[]{}()+-^$|".indexOf(ch) >= 0) {
                regex.append('\\');
            }
            regex.append(ch);
        }
        if (anchored) {
            regex.append("$");
        }
        return regex.toString();
    }

    private static boolean isExtglobMarker(char ch) {
        return ch == '@' || ch == '?' || ch == '+' || ch == '*' || ch == '!';
    }

    private static String toExtglobRegex(char marker, String body, boolean dot) {
        List<String> options = splitPipeOptions(body);
        List<String> optionRegexes = new ArrayList<>();
        for (String option : options) {
            optionRegexes.add(buildPatternRegex(option, dot, false));
        }
        if (optionRegexes.isEmpty()) {
            optionRegexes.add("");
        }
        String union = "(?:" + String.join("|", optionRegexes) + ")";
        switch (marker) {
            case '@':
                return union;
            case '?':
                return "(?:" + union + ")?";
            case '+':
                return "(?:" + union + ")+";
            case '*':
                return "(?:" + union + ")*";
            case '!':
                return "(?:(?!" + union + "(?:/|$))[^/]+)";
            default:
                return Pattern.quote(String.valueOf(marker) + "(" + body + ")");
        }
    }

    private static List<String> expandBraces(String pattern) {
        if (pattern == null || pattern.indexOf('{') < 0) {
            return Collections.singletonList(pattern);
        }
        int open = -1;
        int depth = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '{') {
                if (depth == 0) {
                    open = i;
                }
                depth++;
            } else if (ch == '}') {
                if (depth == 0) {
                    continue;
                }
                depth--;
                if (depth == 0 && open >= 0) {
                    String prefix = pattern.substring(0, open);
                    String body = pattern.substring(open + 1, i);
                    String suffix = pattern.substring(i + 1);
                    List<String> expanded = new ArrayList<>();
                    for (String option : splitBraceOptions(body)) {
                        for (String expandedOption : expandBraces(option + suffix)) {
                            expanded.add(prefix + expandedOption);
                        }
                    }
                    return expanded;
                }
            }
        }
        return Collections.singletonList(pattern);
    }

    private static List<String> splitBraceOptions(String body) {
        List<String> options = new ArrayList<>();
        if (body == null || body.isEmpty()) {
            options.add("");
            return options;
        }
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == ',' && depth == 0) {
                options.add(current.toString());
                current.setLength(0);
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}' && depth > 0) {
                depth--;
            }
            current.append(ch);
        }
        options.add(current.toString());
        return options;
    }

    private static int findClosingParenthesis(String pattern, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitPipeOptions(String body) {
        List<String> options = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '|' && parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                options.add(current.toString());
                current.setLength(0);
                continue;
            }
            if (ch == '(') {
                parenDepth++;
            } else if (ch == ')' && parenDepth > 0) {
                parenDepth--;
            } else if (ch == '{') {
                braceDepth++;
            } else if (ch == '}' && braceDepth > 0) {
                braceDepth--;
            } else if (ch == '[') {
                bracketDepth++;
            } else if (ch == ']' && bracketDepth > 0) {
                bracketDepth--;
            }
            current.append(ch);
        }
        options.add(current.toString());
        return options;
    }

    private static int findClosingBracket(String pattern, int start) {
        for (int i = start; i < pattern.length(); i++) {
            if (pattern.charAt(i) == ']') {
                return i;
            }
        }
        return -1;
    }

    private static String toCharacterClassRegex(String classBody) {
        if (classBody == null || classBody.isEmpty()) {
            return "\\[\\]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        int index = 0;
        char first = classBody.charAt(0);
        if (first == '!' || first == '^') {
            builder.append('^');
            index = 1;
        }
        for (int i = index; i < classBody.length(); i++) {
            char ch = classBody.charAt(i);
            if (ch == '\\') {
                builder.append("\\\\");
                continue;
            }
            if (ch == '[' || ch == ']' || ch == '^') {
                builder.append('\\');
            }
            builder.append(ch);
        }
        builder.append(']');
        return builder.toString();
    }

    private static String normalizePointer(String pointer) {
        if (pointer == null || pointer.trim().isEmpty()) {
            return "/";
        }
        String trimmed = pointer.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
