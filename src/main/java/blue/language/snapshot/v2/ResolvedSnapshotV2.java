package blue.language.snapshot.v2;

import blue.language.blueid.v2.BlueIdIndex;

import java.util.Objects;

public final class ResolvedSnapshotV2 {

    private final FrozenNode canonicalRoot;
    private final FrozenNode resolvedRoot;
    private final String rootBlueId;
    private final BlueIdIndex blueIdsByPointer;

    public ResolvedSnapshotV2(FrozenNode canonicalRoot,
                              FrozenNode resolvedRoot,
                              String rootBlueId,
                              BlueIdIndex blueIdsByPointer) {
        this.canonicalRoot = Objects.requireNonNull(canonicalRoot, "canonicalRoot");
        this.resolvedRoot = Objects.requireNonNull(resolvedRoot, "resolvedRoot");
        this.rootBlueId = Objects.requireNonNull(rootBlueId, "rootBlueId");
        this.blueIdsByPointer = Objects.requireNonNull(blueIdsByPointer, "blueIdsByPointer");
    }

    public FrozenNode canonicalRoot() {
        return canonicalRoot;
    }

    public FrozenNode resolvedRoot() {
        return resolvedRoot;
    }

    public String rootBlueId() {
        return rootBlueId;
    }

    public BlueIdIndex blueIdsByPointer() {
        return blueIdsByPointer;
    }
}
