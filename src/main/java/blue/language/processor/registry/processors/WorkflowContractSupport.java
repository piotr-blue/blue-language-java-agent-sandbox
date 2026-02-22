package blue.language.processor.registry.processors;

import blue.language.model.Node;

final class WorkflowContractSupport {

    private WorkflowContractSupport() {
    }

    static boolean matchesEventFilter(Node event, Node filter) {
        if (filter == null) {
            return true;
        }
        if (event == null) {
            return false;
        }
        Node expectedType = filter.getType();
        if (expectedType == null) {
            return true;
        }
        Node eventType = event.getType();
        if (eventType == null) {
            return false;
        }
        String expectedBlueId = expectedType.getBlueId();
        if (expectedBlueId == null) {
            return true;
        }
        String eventBlueId = eventType.getBlueId();
        return expectedBlueId.equals(eventBlueId);
    }
}
