package blue.language.provider;

import blue.language.NodeProvider;
import blue.language.model.Node;

import java.util.List;

public final class FailOnFetchNodeProvider implements NodeProvider {
    @Override
    public List<Node> fetchByBlueId(String blueId) {
        throw new AssertionError("fetchByBlueId called unexpectedly: " + blueId);
    }
}
