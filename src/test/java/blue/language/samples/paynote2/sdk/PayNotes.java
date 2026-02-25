package blue.language.samples.paynote2.sdk;

public final class PayNotes {

    private PayNotes() {
    }

    public static PayNoteBuilder payNote(String name) {
        return PayNoteBuilder.payNote(name);
    }
}
