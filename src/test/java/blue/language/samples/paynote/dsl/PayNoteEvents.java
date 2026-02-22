package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.function.Consumer;

public final class PayNoteEvents {

    private PayNoteEvents() {
    }

    public static Node reserveFundsRequested(Object amount) {
        return new Node()
                .type(PayNoteAliases.RESERVE_FUNDS_REQUESTED)
                .properties("amount", valueNode(amount));
    }

    public static Node captureFundsRequested(Object amount) {
        return new Node()
                .type(PayNoteAliases.CAPTURE_FUNDS_REQUESTED)
                .properties("amount", valueNode(amount));
    }

    public static Node reservationReleaseRequested(Object amount) {
        return new Node()
                .type(PayNoteAliases.RESERVATION_RELEASE_REQUESTED)
                .properties("amount", valueNode(amount));
    }

    public static Node reserveFundsAndCaptureImmediatelyRequested(Object amount) {
        return new Node()
                .type(PayNoteAliases.RESERVE_FUNDS_AND_CAPTURE_IMMEDIATELY_REQUESTED)
                .properties("amount", valueNode(amount));
    }

    public static Node issueChildPayNoteRequested(Object childPayNoteRef) {
        return new Node()
                .type(PayNoteAliases.ISSUE_CHILD_PAYNOTE_REQUESTED)
                .properties("childPayNote", valueNode(childPayNoteRef));
    }

    public static Node payNoteCancellationRequested(Object childPayNoteRef) {
        return new Node()
                .type(PayNoteAliases.PAYNOTE_CANCELLATION_REQUESTED)
                .properties("childPayNote", valueNode(childPayNoteRef));
    }

    public static CardTransactionCaptureEventBuilder cardTransactionCaptureLockRequested() {
        return new CardTransactionCaptureEventBuilder(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED);
    }

    public static CardTransactionCaptureEventBuilder cardTransactionCaptureUnlockRequested() {
        return new CardTransactionCaptureEventBuilder(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED);
    }

    public static Node cardTransactionCaptureLocked() {
        return new Node().type(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCKED);
    }

    public static Node cardTransactionCaptureUnlocked() {
        return new Node().type(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCKED);
    }

    public static final class CardTransactionCaptureEventBuilder {
        private final Node event;

        private CardTransactionCaptureEventBuilder(String typeAlias) {
            this.event = new Node().type(typeAlias);
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
