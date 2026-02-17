package blue.language.transport.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.utils.NodeToMapListOrValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class BundleBuilderV2 {

    public Map<String, Object> forCanonical(Blue blue, Node canonicalRoot) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(canonicalRoot, "canonicalRoot");

        Set<String> references = new LinkedHashSet<String>();
        collectReferences(canonicalRoot, references);

        Map<String, Object> bundle = new LinkedHashMap<String, Object>();
        for (String blueId : references) {
            List<Node> referencedNodes = blue.getNodeProvider().fetchByBlueId(blueId);
            if (referencedNodes == null || referencedNodes.isEmpty()) {
                continue;
            }

            if (referencedNodes.size() == 1) {
                bundle.put(blueId, NodeToMapListOrValue.get(referencedNodes.get(0)));
            } else {
                List<Object> serializedList = referencedNodes.stream()
                        .map(NodeToMapListOrValue::get)
                        .collect(Collectors.toList());
                bundle.put(blueId, serializedList);
            }
        }
        return bundle;
    }

    private void collectReferences(Node node, Set<String> references) {
        if (node == null) {
            return;
        }

        if (BlueIdCalculatorV2.isPureReferenceNode(node)) {
            references.add(node.getBlueId());
            return;
        }

        collectReferences(node.getType(), references);
        collectReferences(node.getItemType(), references);
        collectReferences(node.getKeyType(), references);
        collectReferences(node.getValueType(), references);
        collectReferences(node.getBlue(), references);

        if (node.getItems() != null) {
            for (Node item : new ArrayList<Node>(node.getItems())) {
                collectReferences(item, references);
            }
        }

        if (node.getProperties() != null) {
            for (Node property : node.getProperties().values()) {
                collectReferences(property, references);
            }
        }
    }
}
