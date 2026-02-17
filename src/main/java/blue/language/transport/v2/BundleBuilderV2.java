package blue.language.transport.v2;

import blue.language.Blue;
import blue.language.model.Node;

import java.util.Map;

public final class BundleBuilderV2 {

    private final blue.language.transport.BundleBuilder delegate = new blue.language.transport.BundleBuilder();

    public Map<String, Object> forCanonical(Blue blue, Node canonicalRoot) {
        return delegate.forCanonical(blue, canonicalRoot);
    }
}
