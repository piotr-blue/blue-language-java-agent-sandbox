package blue.language.samples.paynote.sdk.vnext;

import blue.language.samples.paynote.dsl.PayNoteAliases;

public final class PayNotes {

    private PayNotes() {
    }

    public static PayNoteBuilderVNext payNote(String name) {
        return new PayNoteBuilderVNext(name, PayNoteAliases.PAYNOTE);
    }

    public static PayNoteBuilderVNext cardTransaction(String name) {
        return new PayNoteBuilderVNext(name, PayNoteAliases.CARD_TRANSACTION_PAYNOTE);
    }

    public static PayNoteBuilderVNext shipmentCapture(String name) {
        return new PayNoteBuilderVNext(name, PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE);
    }
}
