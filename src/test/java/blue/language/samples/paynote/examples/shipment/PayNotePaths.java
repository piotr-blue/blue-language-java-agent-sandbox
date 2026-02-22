package blue.language.samples.paynote.examples.shipment;

import blue.language.samples.paynote.dsl.DocPath;

public final class PayNotePaths {

    private PayNotePaths() {
    }

    public static final DocPath CARD_TXN_DETAILS = DocPath.of("/cardTransactionDetails");
    public static final DocPath FUNDING_SOURCE_CURRENCY = DocPath.of("/funding/sourceCurrency");
    public static final DocPath AMOUNT_TOTAL = DocPath.of("/amount/total");
}
