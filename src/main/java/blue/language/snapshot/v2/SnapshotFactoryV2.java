package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdTreeHasherV2;
import blue.language.blueid.v2.BlueIdIndex;
import blue.language.model.Node;

import java.util.Objects;

public final class SnapshotFactoryV2 {

    public ResolvedSnapshotV2 fromAuthoring(Blue blue, Node authoring) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(authoring, "authoring");

        Node preprocessed = blue.preprocess(authoring.clone());
        Node resolved = blue.resolve(preprocessed);
        Node canonical = blue.reverse(resolved.clone());
        return buildSnapshot(canonical, resolved);
    }

    public ResolvedSnapshotV2 fromResolved(Blue blue, Node resolved, SnapshotTrustV2 trust) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolved, "resolved");
        Objects.requireNonNull(trust, "trust");

        Node resolvedNode = trust == SnapshotTrustV2.BLIND_TRUST_RESOLVED
                ? resolved.clone()
                : blue.resolve(resolved.clone());
        Node canonical = blue.reverse(resolvedNode.clone());
        return buildSnapshot(canonical, resolvedNode);
    }

    private ResolvedSnapshotV2 buildSnapshot(Node canonical, Node resolved) {
        BlueIdTreeHasherV2.BlueIdTreeHashResult hashResult = BlueIdTreeHasherV2.hashAndIndex(canonical);
        String rootBlueId = hashResult.rootBlueId();
        BlueIdIndex index = hashResult.index();
        return new ResolvedSnapshotV2(
                FrozenNode.fromNode(canonical),
                FrozenNode.fromNode(resolved),
                rootBlueId,
                index
        );
    }

}
