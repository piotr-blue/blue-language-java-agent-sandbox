package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

public final class PayNoteEvents {

    private PayNoteEvents() {
    }

    public static Node reserveFundsRequested(Object amount) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.ReserveFundsRequested.class).asTypeNode())
                .properties("amount", valueNode(amount));
    }

    public static Node captureFundsRequested(Object amount) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.CaptureFundsRequested.class).asTypeNode())
                .properties("amount", valueNode(amount));
    }

    public static Node reservationReleaseRequested(Object amount) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.ReservationReleaseRequested.class).asTypeNode())
                .properties("amount", valueNode(amount));
    }

    public static Node reserveFundsAndCaptureImmediatelyRequested(Object amount) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.ReserveFundsAndCaptureImmediatelyRequested.class).asTypeNode())
                .properties("amount", valueNode(amount));
    }

    public static Node issueChildPayNoteRequested(Object childPayNoteRef) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.IssueChildPayNoteRequested.class).asTypeNode())
                .properties("childPayNote", valueNode(childPayNoteRef));
    }

    public static Node payNoteCancellationRequested(Object childPayNoteRef) {
        return new Node()
                .type(TypeRef.of(PayNoteTypes.PayNoteCancellationRequested.class).asTypeNode())
                .properties("childPayNote", valueNode(childPayNoteRef));
    }

    public static Node captureLockRequested() {
        return new Node().type(TypeRef.of(PayNoteTypes.CaptureLockRequested.class).asTypeNode());
    }

    public static Node captureUnlockRequested() {
        return new Node().type(TypeRef.of(PayNoteTypes.CaptureUnlockRequested.class).asTypeNode());
    }

    public static Node captureLocked() {
        return new Node().type(TypeRef.of(PayNoteTypes.CaptureLocked.class).asTypeNode());
    }

    public static Node captureUnlocked() {
        return new Node().type(TypeRef.of(PayNoteTypes.CaptureUnlocked.class).asTypeNode());
    }

    private static Node valueNode(Object value) {
        if (value instanceof Node) {
            return (Node) value;
        }
        return new Node().value(value);
    }
}
