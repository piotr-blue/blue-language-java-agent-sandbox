package blue.language.samples.paynote.types.paynote;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class PayNoteV2Types {

    private PayNoteV2Types() {
    }

    @TypeAlias("PayNote/PayNote")
    @TypeBlueId("PayNote-Document-V2-Demo-BlueId")
    public static class PayNoteDocument {
    }
}
