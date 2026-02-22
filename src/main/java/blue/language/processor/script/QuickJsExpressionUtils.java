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

    private static final Pattern STANDALONE_EXPRESSION_PATTERN = Pattern.compile("^\\$\\{[^{}]+}$");
    private static final Pattern TEMPLATE_EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public interface PointerPredicate {
        boolean test(String pointer, Node node);
    }

    private QuickJsExpressionUtils() {
    }

    public static boolean isExpression(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String text = ((String) value).trim();
        return STANDALONE_EXPRESSION_PATTERN.matcher(text).matches();
    }

    public static boolean containsExpression(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        return TEMPLATE_EXPRESSION_PATTERN.matcher((String) value).find();
    }

    public static String extractExpressionContent(String expression) {
        if (!isExpression(expression)) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        String text = expression.trim();
        return text.substring(2, text.length() - 1);
    }

    public static PointerPredicate createPathPredicate(List<String> includePatterns, List<String> excludePatterns) {
        final List<String> includes = includePatterns == null || includePatterns.isEmpty()
                ? Collections.singletonList("/**")
                : includePatterns;
        final List<String> excludes = excludePatterns == null ? Collections.<String>emptyList() : excludePatterns;
        return new PointerPredicate() {
            @Override
            public boolean test(String pointer, Node node) {
                String normalized = normalizePointer(pointer);
                boolean included = false;
                for (String pattern : includes) {
                    if (matchesPattern(normalized, pattern)) {
                        included = true;
                        break;
                    }
                }
                if (!included) {
                    return false;
                }
                for (String pattern : excludes) {
                    if (matchesPattern(normalized, pattern)) {
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
        return resolveRecursive(node, "", evaluator, bindings, context, shouldResolve, shouldDescend, wasmGasConsumer);
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
                String childPointer = pointer + "/" + escapeSegment(entry.getKey());
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
                String childPointer = pointer + "/" + i;
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
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(value)));
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

    private static boolean matchesPattern(String pointer, String pattern) {
        String normalizedPattern = normalizePointer(pattern);
        String regex = normalizedPattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("**", "___DOUBLE_STAR___")
                .replace("*", "[^/]*")
                .replace("___DOUBLE_STAR___", ".*");
        return pointer.matches(regex);
    }

    private static String normalizePointer(String pointer) {
        if (pointer == null || pointer.trim().isEmpty()) {
            return "/";
        }
        String trimmed = pointer.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
