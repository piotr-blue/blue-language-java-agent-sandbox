package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.utils.NodeToMapListOrValue;
import blue.language.utils.UncheckedObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

final class QuickJSStepBindings {

    private QuickJSStepBindings() {
    }

    static Map<String, Object> create(StepExecutionArgs args) {
        Map<String, Object> bindings = new LinkedHashMap<>();
        Object eventJson = NodeToMapListOrValue.get(args.eventNode());
        bindings.put("event", eventJson);
        bindings.put("eventCanonical", eventJson);
        bindings.put("steps", args.stepResults());
        Node documentSnapshot = args.context().documentAt("/");
        args.context().chargeDocumentSnapshot("/", documentSnapshot);
        bindings.put("__documentData", documentSnapshot != null ? NodeToMapListOrValue.get(documentSnapshot) : null);
        Map<?, ?> currentContract = UncheckedObjectMapper.JSON_MAPPER.convertValue(args.workflow(), Map.class);
        bindings.put("currentContract", currentContract);
        bindings.put("currentContractCanonical", currentContract);
        return bindings;
    }
}
