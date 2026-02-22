package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.ProcessorGasSchedule;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.script.QuickJSEvaluator;
import blue.language.processor.script.ScriptRuntimeResult;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;
import blue.language.utils.NodeToMapListOrValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JavaScriptCodeStepExecutor implements WorkflowStepExecutor {

    private final QuickJSEvaluator evaluator;

    public JavaScriptCodeStepExecutor() {
        this(new QuickJSEvaluator());
    }

    public JavaScriptCodeStepExecutor(QuickJSEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Set<String> supportedBlueIds() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
                "Conversation/JavaScript Code",
                "JavaScriptCode"
        )));
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        String code = readCode(args.stepNode());
        if (code == null || code.trim().isEmpty()) {
            args.context().throwFatal("JavaScript Code step requires non-empty code");
            return null;
        }
        ScriptRuntimeResult runtimeResult = evaluator.evaluate(
                code,
                createBindings(args),
                ProcessorGasSchedule.DEFAULT_WASM_GAS_LIMIT);
        args.context().chargeWasmGas(runtimeResult.wasmGasUsed());
        Object result = runtimeResult.value();
        applyRuntimeEffects(args, result);
        return result;
    }

    private Map<String, Object> createBindings(StepExecutionArgs args) {
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("event", NodeToMapListOrValue.get(args.eventNode()));
        bindings.put("steps", args.stepResults());
        Node documentSnapshot = args.context().documentAt("/");
        args.context().chargeDocumentSnapshot("/", documentSnapshot);
        bindings.put("document", documentSnapshot != null ? NodeToMapListOrValue.get(documentSnapshot) : null);
        return bindings;
    }

    @SuppressWarnings("unchecked")
    private void applyRuntimeEffects(StepExecutionArgs args, Object result) {
        if (!(result instanceof Map)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) result;
        Object emit = map.get("emit");
        if (emit != null) {
            args.context().emitEvent(toNode(emit));
        }

        Object gas = map.get("consumeGas");
        if (gas instanceof Number) {
            args.context().consumeGas(((Number) gas).longValue());
        } else if (gas instanceof String) {
            try {
                args.context().consumeGas(new BigInteger((String) gas).longValue());
            } catch (NumberFormatException ignored) {
                // ignore invalid consumeGas hints
            }
        }

        Object changeset = map.get("changeset");
        if (!(changeset instanceof List)) {
            return;
        }
        for (Object rawChange : (List<Object>) changeset) {
            if (!(rawChange instanceof Map)) {
                continue;
            }
            applyPatchChange(args, (Map<String, Object>) rawChange);
        }
    }

    private void applyPatchChange(StepExecutionArgs args, Map<String, Object> change) {
        Object rawOp = change.get("op");
        String op = rawOp != null ? String.valueOf(rawOp).trim().toUpperCase(Locale.ROOT) : "REPLACE";
        Object rawPath = change.get("path");
        if (rawPath == null || String.valueOf(rawPath).trim().isEmpty()) {
            args.context().throwFatal("JavaScript Code changeset entries require path");
            return;
        }
        String absolutePath = args.context().resolvePointer(String.valueOf(rawPath));
        if ("REMOVE".equals(op)) {
            args.context().applyPatch(JsonPatch.remove(absolutePath));
            return;
        }
        Node val = toNode(change.get("val"));
        if ("ADD".equals(op)) {
            args.context().applyPatch(JsonPatch.add(absolutePath, val));
            return;
        }
        if ("REPLACE".equals(op)) {
            args.context().applyPatch(JsonPatch.replace(absolutePath, val));
            return;
        }
        args.context().throwFatal("Unsupported JavaScript Code patch operation \"" + op + "\"");
    }

    private String readCode(Node stepNode) {
        if (stepNode == null) {
            return null;
        }
        if (stepNode.getValue() instanceof String) {
            return String.valueOf(stepNode.getValue());
        }
        if (stepNode.getProperties() == null) {
            return null;
        }
        Node code = stepNode.getProperties().get("code");
        if (code == null || code.getValue() == null) {
            return null;
        }
        return String.valueOf(code.getValue());
    }

    private Node toNode(Object value) {
        if (value instanceof Node) {
            return ((Node) value).clone();
        }
        if (value instanceof Map) {
            Node node = new Node();
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                node.properties(String.valueOf(entry.getKey()), toNode(entry.getValue()));
            }
            return node;
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            List<Node> items = new ArrayList<>();
            for (Object item : list) {
                items.add(toNode(item));
            }
            return new Node().items(items);
        }
        return new Node().value(value);
    }
}
