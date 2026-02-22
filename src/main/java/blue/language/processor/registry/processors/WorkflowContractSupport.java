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
        return matchesNode(event, filter);
    }

    private static boolean matchesNode(Node candidate, Node pattern) {
        if (pattern == null) {
            return true;
        }
        if (candidate == null) {
            return false;
        }

        if (!matchesType(candidate, pattern)) {
            return false;
        }
        if (pattern.getValue() != null) {
            if (candidate.getValue() == null) {
                return false;
            }
            if (!pattern.getValue().equals(candidate.getValue())) {
                return false;
            }
        }

        if (pattern.getProperties() != null && !pattern.getProperties().isEmpty()) {
            if (candidate.getProperties() == null) {
                return false;
            }
            for (java.util.Map.Entry<String, Node> entry : pattern.getProperties().entrySet()) {
                if (!candidate.getProperties().containsKey(entry.getKey())) {
                    return false;
                }
                if (!matchesNode(candidate.getProperties().get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
        }

        if (pattern.getItems() != null && !pattern.getItems().isEmpty()) {
            if (candidate.getItems() == null || candidate.getItems().size() < pattern.getItems().size()) {
                return false;
            }
            for (int i = 0; i < pattern.getItems().size(); i++) {
                Node patternItem = pattern.getItems().get(i);
                if (patternItem == null) {
                    continue;
                }
                if (!matchesNode(candidate.getItems().get(i), patternItem)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean matchesType(Node candidate, Node pattern) {
        Node expectedType = pattern.getType();
        if (expectedType == null) {
            return true;
        }
        Node candidateType = candidate.getType();
        if (candidateType == null) {
            return false;
        }
        String expectedBlueId = expectedType.getBlueId();
        if (expectedBlueId == null || expectedBlueId.trim().isEmpty()) {
            return true;
        }
        String candidateBlueId = candidateType.getBlueId();
        return expectedBlueId.equals(candidateBlueId);
    }
}
