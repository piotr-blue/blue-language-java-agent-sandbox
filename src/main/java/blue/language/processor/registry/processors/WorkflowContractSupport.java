package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.utils.Properties;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    static boolean matchesTypeRequirement(Node candidate, Node requirement) {
        if (candidate == null || requirement == null) {
            return false;
        }
        if (!matchesType(candidate, requirement)) {
            return false;
        }
        if (requirement.getValue() != null) {
            if (candidate.getValue() == null || !requirement.getValue().equals(candidate.getValue())) {
                return false;
            }
        }

        Node entries = requirement.getProperties() != null ? requirement.getProperties().get("entries") : null;
        if (entries != null && entries.getProperties() != null && !entries.getProperties().isEmpty()) {
            if (candidate.getProperties() == null) {
                return false;
            }
            for (java.util.Map.Entry<String, Node> entry : entries.getProperties().entrySet()) {
                Node candidateEntry = candidate.getProperties().get(entry.getKey());
                if (!matchesTypeRequirement(candidateEntry, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }

        Node itemType = requirement.getProperties() != null ? requirement.getProperties().get("itemType") : null;
        if (itemType != null) {
            if (candidate.getItems() == null) {
                return false;
            }
            for (Node item : candidate.getItems()) {
                if (!matchesTypeRequirement(item, itemType)) {
                    return false;
                }
            }
            return true;
        }

        return true;
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
        String expectedBlueId = expectedType.getBlueId();
        if (expectedBlueId == null || expectedBlueId.trim().isEmpty()) {
            return true;
        }

        String candidateBlueId = candidate.getType() != null ? candidate.getType().getBlueId() : null;
        if (candidateBlueId != null && !candidateBlueId.trim().isEmpty()) {
            return expectedBlueId.equals(candidateBlueId) || equivalentCoreType(expectedBlueId, candidateBlueId);
        }

        String inferredCandidateType = inferCoreType(candidate);
        if (inferredCandidateType == null) {
            return false;
        }
        return expectedBlueId.equals(inferredCandidateType) || equivalentCoreType(expectedBlueId, inferredCandidateType);
    }

    private static boolean equivalentCoreType(String left, String right) {
        String normalizedLeft = normalizeCoreType(left);
        String normalizedRight = normalizeCoreType(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private static String normalizeCoreType(String typeId) {
        if (typeId == null) {
            return null;
        }
        if (Properties.INTEGER_TYPE.equals(typeId) || Properties.INTEGER_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.INTEGER_TYPE;
        }
        if (Properties.DOUBLE_TYPE.equals(typeId) || Properties.DOUBLE_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.DOUBLE_TYPE;
        }
        if (Properties.BOOLEAN_TYPE.equals(typeId) || Properties.BOOLEAN_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.BOOLEAN_TYPE;
        }
        if (Properties.TEXT_TYPE.equals(typeId) || Properties.TEXT_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.TEXT_TYPE;
        }
        if (Properties.LIST_TYPE.equals(typeId) || Properties.LIST_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.LIST_TYPE;
        }
        if (Properties.DICTIONARY_TYPE.equals(typeId) || Properties.DICTIONARY_TYPE_BLUE_ID.equals(typeId)) {
            return Properties.DICTIONARY_TYPE;
        }
        return null;
    }

    private static String inferCoreType(Node candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate.getItems() != null) {
            return Properties.LIST_TYPE;
        }
        if (candidate.getProperties() != null && !candidate.getProperties().isEmpty()) {
            return Properties.DICTIONARY_TYPE;
        }
        Object value = candidate.getValue();
        if (value instanceof String) {
            return Properties.TEXT_TYPE;
        }
        if (value instanceof Boolean) {
            return Properties.BOOLEAN_TYPE;
        }
        if (value instanceof BigInteger
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Short
                || value instanceof Byte) {
            return Properties.INTEGER_TYPE;
        }
        if (value instanceof BigDecimal
                || value instanceof Double
                || value instanceof Float) {
            return Properties.DOUBLE_TYPE;
        }
        return null;
    }
}
