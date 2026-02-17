package blue.language.blueid.v2;

import blue.language.model.Node;
import blue.language.utils.NodeToMapListOrValue;

import java.util.List;
import java.util.stream.Collectors;

public final class CanonicalizerV2 {

    private CanonicalizerV2() {
    }

    public static Object toCanonicalObject(Node canonicalNode) {
        return NodeToMapListOrValue.get(canonicalNode);
    }

    public static Object toCanonicalObject(List<Node> canonicalDocs) {
        return canonicalDocs.stream()
                .map(CanonicalizerV2::toCanonicalObject)
                .collect(Collectors.toList());
    }
}
