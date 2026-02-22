package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.utils.NodeToMapListOrValue;
import blue.language.utils.NodeToMapListOrValue.Strategy;
import blue.language.utils.UncheckedObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

final class QuickJSStepBindings {

    private QuickJSStepBindings() {
    }

    static Map<String, Object> create(StepExecutionArgs args) {
        Map<String, Object> bindings = new LinkedHashMap<>();
        Object eventSimple = NodeToMapListOrValue.get(args.eventNode(), Strategy.SIMPLE);
        Object eventCanonical = NodeToMapListOrValue.get(args.eventNode(), Strategy.OFFICIAL);
        bindings.put("event", eventSimple);
        bindings.put("eventCanonical", eventCanonical);
        bindings.put("steps", args.stepResults());
        Node documentSnapshot = args.context().documentAt("/");
        args.context().chargeDocumentSnapshot("/", documentSnapshot);
        bindings.put("__documentDataSimple", documentSnapshot != null
                ? NodeToMapListOrValue.get(documentSnapshot, Strategy.SIMPLE)
                : null);
        bindings.put("__documentDataCanonical", documentSnapshot != null
                ? NodeToMapListOrValue.get(documentSnapshot, Strategy.OFFICIAL)
                : null);
        bindings.put("__scopePath", args.context().resolvePointer("/"));
        Map<?, ?> currentContract = UncheckedObjectMapper.JSON_MAPPER.convertValue(args.workflow(), Map.class);
        bindings.put("currentContract", currentContract);
        bindings.put("currentContractCanonical", currentContract);
        return bindings;
    }
}
