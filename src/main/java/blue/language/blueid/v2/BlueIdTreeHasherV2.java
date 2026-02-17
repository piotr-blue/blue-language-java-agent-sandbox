package blue.language.blueid.v2;

import blue.language.model.Node;
import java.util.function.Function;

public final class BlueIdTreeHasherV2 {

    private BlueIdTreeHasherV2() {
    }

    public static BlueIdTreeHashResult hashAndIndex(Node canonicalRoot) {
        return adapt(blue.language.blueid.BlueIdTreeHasher.hashAndIndex(canonicalRoot));
    }

    static BlueIdTreeHashResult hashAndIndex(Node canonicalRoot, Function<Object, String> hashProvider) {
        return adapt(blue.language.blueid.BlueIdTreeHasher.hashAndIndex(canonicalRoot, hashProvider));
    }

    private static BlueIdTreeHashResult adapt(blue.language.blueid.BlueIdTreeHasher.BlueIdTreeHashResult modern) {
        return new BlueIdTreeHashResult(
                modern.rootBlueId(),
                MapBlueIdIndex.from(modern.index().asMap())
        );
    }

    public static final class BlueIdTreeHashResult {
        private final String rootBlueId;
        private final BlueIdIndex index;

        BlueIdTreeHashResult(String rootBlueId, BlueIdIndex index) {
            this.rootBlueId = rootBlueId;
            this.index = index;
        }

        public String rootBlueId() {
            return rootBlueId;
        }

        public BlueIdIndex index() {
            return index;
        }
    }
}
