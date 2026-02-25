package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.sdk.DocBuilder;

public final class ShipmentPayNoteNodeChaining {

    private ShipmentPayNoteNodeChaining() {
    }

    public static Node specializeEur200ChfDhl(Node templateNode) {
        return DocBuilder.edit(templateNode)
                .withName("Shipment Escrow — EUR 200 via DHL")
                .set("/currency", "EUR")
                .set("/amount/total", 20000)
                .set(PayNotePaths.FUNDING_SOURCE_CURRENCY.pointer(), "CHF")
                .buildDocument();
    }

    public static Node instantiateAliceBob(Node specializedNode) {
        return MyOsDsl.bootstrap(specializedNode)
                .bind("shipmentCompanyChannel").accountId("acc_dhl_001")
                .bind("payerChannel").email("alice@gmail.com")
                .bind("payeeChannel").accountId("acc_bob_1234")
                .bind("guarantorChannel").accountId("acc_bank_1")
                .build();
    }
}
