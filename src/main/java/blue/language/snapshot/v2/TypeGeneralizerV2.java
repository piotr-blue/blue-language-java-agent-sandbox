package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;
import java.util.Objects;

public final class TypeGeneralizerV2 {

    private final blue.language.snapshot.TypeGeneralizer delegate = new blue.language.snapshot.TypeGeneralizer();

    public GeneralizationReport generalizeToSoundness(Blue blue, FrozenNode resolvedRoot, String changedPointer) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolvedRoot, "resolvedRoot");
        blue.language.snapshot.GeneralizationReport report =
                delegate.generalizeToSoundness(blue, SnapshotAdapters.toModern(resolvedRoot), changedPointer);
        return SnapshotAdapters.toLegacy(report);
    }

    public GeneralizationReport generalizeToSoundness(Blue blue, Node mutableResolvedRoot, String changedPointer) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(mutableResolvedRoot, "mutableResolvedRoot");
        blue.language.snapshot.GeneralizationReport report =
                delegate.generalizeToSoundness(blue, mutableResolvedRoot, changedPointer);
        return SnapshotAdapters.toLegacy(report);
    }

}
