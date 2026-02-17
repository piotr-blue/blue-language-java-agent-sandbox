package blue.language.snapshot.v2;

final class SnapshotAdapters {

    private SnapshotAdapters() {
    }

    static blue.language.snapshot.ResolvedSnapshot toModern(ResolvedSnapshotV2 legacy) {
        return new blue.language.snapshot.ResolvedSnapshot(
                toModern(legacy.canonicalRoot()),
                toModern(legacy.resolvedRoot()),
                legacy.rootBlueId(),
                blue.language.blueid.MapBlueIdIndex.from(legacy.blueIdsByPointer().asMap())
        );
    }

    static ResolvedSnapshotV2 toLegacy(blue.language.snapshot.ResolvedSnapshot modern) {
        return new ResolvedSnapshotV2(
                toLegacy(modern.canonicalRoot()),
                toLegacy(modern.resolvedRoot()),
                modern.rootBlueId(),
                blue.language.blueid.v2.MapBlueIdIndex.from(modern.blueIdsByPointer().asMap())
        );
    }

    static blue.language.snapshot.FrozenNode toModern(FrozenNode legacy) {
        return blue.language.snapshot.FrozenNode.fromNode(legacy.toNode());
    }

    static FrozenNode toLegacy(blue.language.snapshot.FrozenNode modern) {
        return FrozenNode.fromNode(modern.toNode());
    }

    static GeneralizationReport toLegacy(blue.language.snapshot.GeneralizationReport modern) {
        return new GeneralizationReport(modern.generalizations());
    }

    static PatchReport toLegacy(blue.language.snapshot.PatchReport modern) {
        return new PatchReport(modern.appliedPaths(), toLegacy(modern.generalizationReport()));
    }
}
