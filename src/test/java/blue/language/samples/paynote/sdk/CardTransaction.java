package blue.language.samples.paynote.sdk;

import blue.language.samples.paynote.dsl.DocPath;

public final class CardTransaction {

    private final DocPath detailsPath;

    private CardTransaction(DocPath detailsPath) {
        this.detailsPath = detailsPath;
    }

    public static CardTransaction at(DocPath detailsPath) {
        return new CardTransaction(detailsPath);
    }

    public static CardTransaction defaultRef() {
        return at(DocPath.of("/cardTransactionDetails"));
    }

    DocPath detailsPath() {
        return detailsPath;
    }
}
