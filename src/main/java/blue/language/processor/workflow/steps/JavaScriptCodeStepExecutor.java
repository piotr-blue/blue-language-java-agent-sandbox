package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.ProcessorGasSchedule;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.script.QuickJSEvaluator;
import blue.language.processor.script.ScriptRuntimeResult;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.WorkflowStepExecutor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JavaScriptCodeStepExecutor implements WorkflowStepExecutor {

    private static final Set<String> SUPPORTED_BLUE_IDS =
            Collections.unmodifiableSet(new LinkedHashSet<String>(java.util.Arrays.asList(
                    "Conversation/JavaScript Code",
                    "JavaScriptCode"
            )));

    private final QuickJSEvaluator evaluator;

    public JavaScriptCodeStepExecutor() {
        this(new QuickJSEvaluator());
    }

    public JavaScriptCodeStepExecutor(QuickJSEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public Set<String> supportedBlueIds() {
        return SUPPORTED_BLUE_IDS;
    }

    @Override
    public Object execute(StepExecutionArgs args) {
        if (!isValidStepNode(args.stepNode())) {
            args.context().throwFatal("JavaScript Code step payload is invalid");
            return null;
        }
        String code = readCode(args.stepNode());
        if (code == null || code.trim().isEmpty()) {
            args.context().throwFatal("JavaScript Code step requires non-empty code");
            return null;
        }
        ScriptRuntimeResult runtimeResult = evaluator.evaluate(
                code,
                QuickJSStepBindings.create(args),
                ProcessorGasSchedule.DEFAULT_WASM_GAS_LIMIT);
        args.context().chargeWasmGas(runtimeResult.wasmGasUsed());
        Object result = runtimeResult.value();
        handleEvents(args, result);
        applyRuntimeEffects(args, result);
        return unwrapResult(result);
    }

    @SuppressWarnings("unchecked")
    private void applyRuntimeEffects(StepExecutionArgs args, Object result) {
        if (!(result instanceof Map)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) result;
        Object emit = map.get("emit");
        if (emit != null) {
            args.context().emitEvent(toEventNode(emit));
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

    @SuppressWarnings("unchecked")
    private void handleEvents(StepExecutionArgs args, Object result) {
        if (!(result instanceof Map)) {
            return;
        }
        Object events = ((Map<String, Object>) result).get("events");
        if (!(events instanceof List)) {
            return;
        }
        for (Object event : (List<Object>) events) {
            args.context().emitEvent(toEventNode(event));
        }
    }

    private Node toEventNode(Object value) {
        Node node = toNode(value);
        if (node == null || node.getType() != null || node.getProperties() == null) {
            return node;
        }
        Node typeNode = node.getProperties().get("type");
        if (typeNode == null) {
            return node;
        }
        String blueId = extractBlueId(typeNode);
        if (blueId != null && !blueId.trim().isEmpty()) {
            node.type(new Node().blueId(blueId.trim()));
        }
        return node;
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

    private String extractBlueId(Node typeNode) {
        if (typeNode == null) {
            return null;
        }
        if (typeNode.getBlueId() != null && !typeNode.getBlueId().trim().isEmpty()) {
            return typeNode.getBlueId().trim();
        }
        if (typeNode.getValue() instanceof String) {
            String value = ((String) typeNode.getValue()).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (typeNode.getProperties() != null && typeNode.getProperties().get("blueId") != null) {
            Node blueIdNode = typeNode.getProperties().get("blueId");
            if (blueIdNode != null && blueIdNode.getValue() != null) {
                String value = String.valueOf(blueIdNode.getValue()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object unwrapResult(Object result) {
        if (!(result instanceof Map)) {
            return result;
        }
        Map<String, Object> map = (Map<String, Object>) result;
        if (map.containsKey("__result")) {
            return map.get("__result");
        }
        return result;
    }

    private boolean isValidStepNode(Node stepNode) {
        if (stepNode == null || stepNode.getType() == null) {
            return false;
        }
        return hasSupportedType(stepNode.getType(), new LinkedHashSet<String>());
    }

    private boolean hasSupportedType(Node typeNode, Set<String> visitedBlueIds) {
        if (typeNode == null) {
            return false;
        }
        boolean foundUnvisitedBlueId = false;
        for (String blueId : extractBlueIds(typeNode)) {
            if (SUPPORTED_BLUE_IDS.contains(blueId)) {
                return true;
            }
            if (visitedBlueIds.add(blueId)) {
                foundUnvisitedBlueId = true;
            }
        }
        Node parentType = typeNode.getType();
        if (parentType == null) {
            return false;
        }
        if (!foundUnvisitedBlueId) {
            List<String> parentBlueIds = extractBlueIds(parentType);
            boolean parentHasUnvisitedBlueId = false;
            for (String blueId : parentBlueIds) {
                if (!visitedBlueIds.contains(blueId)) {
                    parentHasUnvisitedBlueId = true;
                    break;
                }
            }
            if (!parentHasUnvisitedBlueId && parentBlueIds.isEmpty()) {
                return false;
            }
        }
        return hasSupportedType(parentType, visitedBlueIds);
    }

    private List<String> extractBlueIds(Node typeNode) {
        List<String> blueIds = new ArrayList<>();
        addBlueId(blueIds, typeNode.getBlueId());
        if (typeNode.getValue() instanceof String) {
            addBlueId(blueIds, String.valueOf(typeNode.getValue()));
        }
        if (typeNode.getProperties() != null && typeNode.getProperties().get("blueId") != null) {
            Node blueIdNode = typeNode.getProperties().get("blueId");
            if (blueIdNode != null && blueIdNode.getValue() != null) {
                addBlueId(blueIds, String.valueOf(blueIdNode.getValue()));
            }
        }
        return blueIds;
    }

    private void addBlueId(List<String> blueIds, String blueId) {
        if (blueId == null) {
            return;
        }
        String normalized = blueId.trim();
        if (normalized.isEmpty() || blueIds.contains(normalized)) {
            return;
        }
        blueIds.add(normalized);
    }
}
