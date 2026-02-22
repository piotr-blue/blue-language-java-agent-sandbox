package blue.language.processor.workflow.steps;

import blue.language.model.Node;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.utils.NodeToMapListOrValue;

import java.util.LinkedHashMap;
import java.util.Map;

final class QuickJSStepBindings {

    private QuickJSStepBindings() {
    }

    static Map<String, Object> create(StepExecutionArgs args) {
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("event", NodeToMapListOrValue.get(args.eventNode()));
        bindings.put("steps", args.stepResults());
        Node documentSnapshot = args.context().documentAt("/");
        args.context().chargeDocumentSnapshot("/", documentSnapshot);
        bindings.put("__documentData", documentSnapshot != null ? NodeToMapListOrValue.get(documentSnapshot) : null);
        return bindings;
    }
}
