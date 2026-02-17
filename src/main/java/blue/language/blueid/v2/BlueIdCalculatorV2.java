package blue.language.blueid.v2;

import blue.language.model.Node;

import java.util.List;

public final class BlueIdCalculatorV2 {

    private BlueIdCalculatorV2() {
    }

    public static String calculateSemanticBlueId(Node canonicalNode) {
        return blue.language.blueid.BlueIdCalculator.calculateSemanticBlueId(canonicalNode);
    }

    public static String calculateSemanticBlueId(List<Node> canonicalDocs) {
        return blue.language.blueid.BlueIdCalculator.calculateSemanticBlueId(canonicalDocs);
    }

    public static boolean isPureReferenceNode(Node node) {
        return blue.language.blueid.BlueIdCalculator.isPureReferenceNode(node);
    }

    public static String rehashPath(Node canonicalRoot, String jsonPointer) {
        return blue.language.blueid.BlueIdCalculator.rehashPath(canonicalRoot, jsonPointer);
    }

}
