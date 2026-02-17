package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.snapshot.SnapshotFactory;
import blue.language.snapshot.SnapshotTrust;

import java.util.Objects;

public final class SnapshotFactoryV2 {

    private final SnapshotFactory delegate = new SnapshotFactory();

    public ResolvedSnapshotV2 fromAuthoring(Blue blue, Node authoring) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(authoring, "authoring");
        return SnapshotAdapters.toLegacy(delegate.fromAuthoring(blue, authoring));
    }

    public ResolvedSnapshotV2 fromResolved(Blue blue, Node resolved, SnapshotTrustV2 trust) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolved, "resolved");
        Objects.requireNonNull(trust, "trust");
        SnapshotTrust mappedTrust = trust == SnapshotTrustV2.BLIND_TRUST_RESOLVED
                ? SnapshotTrust.BLIND_TRUST_RESOLVED
                : SnapshotTrust.RESOLVE;
        return SnapshotAdapters.toLegacy(delegate.fromResolved(blue, resolved, mappedTrust));
    }

}
