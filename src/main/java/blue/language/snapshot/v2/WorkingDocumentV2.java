package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.WorkingDocument;
import java.util.Objects;

public final class WorkingDocumentV2 {

    private final WorkingDocument delegate;

    private WorkingDocumentV2(WorkingDocument delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static WorkingDocumentV2 forSnapshot(Blue blue, ResolvedSnapshotV2 snapshot) {
        WorkingDocument modern = WorkingDocument.forSnapshot(blue, SnapshotAdapters.toModern(snapshot));
        return new WorkingDocumentV2(modern);
    }

    public ResolvedSnapshotV2 snapshot() {
        return SnapshotAdapters.toLegacy(delegate.snapshot());
    }

    public PatchReport applyPatch(JsonPatch patch) {
        return SnapshotAdapters.toLegacy(delegate.applyPatch(patch));
    }

    public ResolvedSnapshotV2 commit() {
        return SnapshotAdapters.toLegacy(delegate.commit());
    }

    public PatchReport lastPatchReport() {
        return SnapshotAdapters.toLegacy(delegate.lastPatchReport());
    }

}
