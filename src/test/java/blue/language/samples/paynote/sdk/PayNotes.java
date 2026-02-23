package blue.language.samples.paynote.sdk;

public final class PayNotes {

    private PayNotes() {
    }

    public static PayNoteBuilder payNote(String name) {
        return PayNoteBuilder.payNote(name);
    }

    public static PayNoteBuilder cardTransaction(String name) {
        return payNote(name).attach(CardTransaction.defaultRef());
    }
}
