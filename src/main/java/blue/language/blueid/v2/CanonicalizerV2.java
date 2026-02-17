package blue.language.blueid.v2;

import blue.language.model.Node;
import java.util.List;

public final class CanonicalizerV2 {

    private CanonicalizerV2() {
    }

    public static Object toCanonicalObject(Node canonicalNode) {
        return blue.language.blueid.Canonicalizer.toCanonicalObject(canonicalNode);
    }

    public static Object toCanonicalObject(List<Node> canonicalDocs) {
        return blue.language.blueid.Canonicalizer.toCanonicalObject(canonicalDocs);
    }
}
