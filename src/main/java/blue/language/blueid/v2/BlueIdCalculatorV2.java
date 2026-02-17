package blue.language.blueid.v2;

import blue.language.model.Node;
import blue.language.utils.BlueIdCalculator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BlueIdCalculatorV2 {

    private BlueIdCalculatorV2() {
    }

    public static String calculateSemanticBlueId(Node canonicalNode) {
        Objects.requireNonNull(canonicalNode, "canonicalNode");
        return BlueIdCalculator.calculateBlueId(canonicalNode);
    }

    public static String calculateSemanticBlueId(List<Node> canonicalDocs) {
        Objects.requireNonNull(canonicalDocs, "canonicalDocs");
        return BlueIdCalculator.calculateBlueId(canonicalDocs);
    }

    public static boolean isPureReferenceNode(Node node) {
        if (node == null || node.getBlueId() == null) {
            return false;
        }

        if (node.getName() != null || node.getDescription() != null || node.getType() != null ||
                node.getItemType() != null || node.getKeyType() != null || node.getValueType() != null ||
                node.getValue() != null || node.getConstraints() != null || node.getBlue() != null) {
            return false;
        }

        if (node.getItems() != null && !node.getItems().isEmpty()) {
            return false;
        }

        Map<String, Node> properties = node.getProperties();
        return properties == null || properties.isEmpty();
    }

    public static String rehashPath(Node canonicalRoot, String jsonPointer) {
        Objects.requireNonNull(canonicalRoot, "canonicalRoot");
        return calculateSemanticBlueId(canonicalRoot);
    }
}
