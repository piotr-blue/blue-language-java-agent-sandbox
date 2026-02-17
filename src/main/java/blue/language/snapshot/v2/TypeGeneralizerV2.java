package blue.language.snapshot.v2;

import blue.language.Blue;

import java.util.Objects;

public final class TypeGeneralizerV2 {

    public GeneralizationReport generalizeToSoundness(Blue blue, FrozenNode resolvedRoot, String changedPointer) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolvedRoot, "resolvedRoot");
        return GeneralizationReport.none();
    }
}
