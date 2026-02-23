package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNoteBuilder;
import blue.language.samples.paynote.sdk.PayNotes;

public final class MyPayNote {

    private MyPayNote() {
    }

    public static PayNoteBuilder base(String name) {
        return PayNotes.payNote(name)
                .currency(IsoCurrency.USD)
                .amountTotalMajor("49.99")
                .reserveOnInit()
                .requestCancellationOperation("payerChannel");
    }

    public static Node baseDocument(String name) {
        return base(name).buildDocument();
    }

    public static Node withExtraOperations(String name) {
        return base(name)
                .operation("supportNote")
                    .channel("payeeChannel")
                    .description("Allow payee support notes.")
                    .steps(steps -> steps.replaceExpression("SetSupportNote", "/support/note", "request.note"))
                    .done()
                .buildDocument();
    }
}
