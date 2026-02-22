package blue.language.processor.script;

import blue.language.model.Node;
import blue.language.processor.ProcessorGasSchedule;
import blue.language.processor.ProcessorExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class QuickJsExpressionUtils {

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
        return text.startsWith("${") && text.endsWith("}");
    }

    public static Node resolveExpressions(Node node,
                                          QuickJSEvaluator evaluator,
                                          Map<String, Object> bindings,
                                          ProcessorExecutionContext context,
                                          PointerPredicate shouldResolve,
                                          PointerPredicate shouldDescend) {
        if (node == null) {
            return null;
        }
        return resolveRecursive(node, "", evaluator, bindings, context, shouldResolve, shouldDescend);
    }

    private static Node resolveRecursive(Node node,
                                         String pointer,
                                         QuickJSEvaluator evaluator,
                                         Map<String, Object> bindings,
                                         ProcessorExecutionContext context,
                                         PointerPredicate shouldResolve,
                                         PointerPredicate shouldDescend) {
        Node cloned = node.clone();
        Object value = cloned.getValue();
        if (isExpression(value) && (shouldResolve == null || shouldResolve.test(pointer, cloned))) {
            String expression = String.valueOf(value).trim();
            String code = expression.substring(2, expression.length() - 1);
            ScriptRuntimeResult runtimeResult = evaluator.evaluate(
                    code,
                    bindings,
                    ProcessorGasSchedule.DEFAULT_EXPRESSION_WASM_GAS_LIMIT);
            context.chargeWasmGas(runtimeResult.wasmGasUsed());
            return toNode(runtimeResult.value());
        }

        if (cloned.getProperties() != null) {
            for (Map.Entry<String, Node> entry : cloned.getProperties().entrySet()) {
                String childPointer = pointer + "/" + escapeSegment(entry.getKey());
                if (shouldDescend != null && !shouldDescend.test(childPointer, entry.getValue())) {
                    continue;
                }
                entry.setValue(resolveRecursive(entry.getValue(), childPointer, evaluator, bindings, context, shouldResolve, shouldDescend));
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
                updatedItems.add(resolveRecursive(child, childPointer, evaluator, bindings, context, shouldResolve, shouldDescend));
            }
            cloned.items(updatedItems);
        }
        return cloned;
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
}
