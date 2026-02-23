package blue.language.samples.paynote.sdk;

import blue.language.samples.paynote.dsl.DocPath;

public final class BankTransfer {

    private final DocPath detailsPath;

    private BankTransfer(DocPath detailsPath) {
        this.detailsPath = detailsPath;
    }

    public static BankTransfer at(DocPath detailsPath) {
        return new BankTransfer(detailsPath);
    }

    DocPath detailsPath() {
        return detailsPath;
    }
}
