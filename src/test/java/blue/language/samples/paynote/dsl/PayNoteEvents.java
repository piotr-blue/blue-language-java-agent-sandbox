package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

import java.util.function.Consumer;

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

    public static CardTransactionCaptureEventBuilder cardTransactionCaptureLockRequested() {
        return new CardTransactionCaptureEventBuilder(PayNoteTypes.CardTransactionCaptureLockRequested.class);
    }

    public static CardTransactionCaptureEventBuilder cardTransactionCaptureUnlockRequested() {
        return new CardTransactionCaptureEventBuilder(PayNoteTypes.CardTransactionCaptureUnlockRequested.class);
    }

    public static Node cardTransactionCaptureLocked() {
        return new Node().type(TypeRef.of(PayNoteTypes.CardTransactionCaptureLocked.class).asTypeNode());
    }

    public static Node cardTransactionCaptureUnlocked() {
        return new Node().type(TypeRef.of(PayNoteTypes.CardTransactionCaptureUnlocked.class).asTypeNode());
    }

    public static final class CardTransactionCaptureEventBuilder {
        private final Node event;

        private CardTransactionCaptureEventBuilder(Class<?> typeClass) {
            this.event = new Node().type(TypeRef.of(typeClass).asTypeNode());
        }

        public CardTransactionCaptureEventBuilder cardTransactionDetails(Consumer<NodeObjectBuilder> customizer) {
            NodeObjectBuilder detailsBuilder = NodeObjectBuilder.create();
            customizer.accept(detailsBuilder);
            event.properties("cardTransactionDetails", detailsBuilder.build());
            return this;
        }

        public CardTransactionCaptureEventBuilder cardTransactionDetails(Node details) {
            event.properties("cardTransactionDetails", details);
            return this;
        }

        public Node build() {
            return event;
        }
    }

    private static Node valueNode(Object value) {
        if (value instanceof Node) {
            return (Node) value;
        }
        return new Node().value(value);
    }
}
