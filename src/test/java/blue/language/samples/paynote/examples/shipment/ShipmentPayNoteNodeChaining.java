package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.DocTemplates;

public final class ShipmentPayNoteNodeChaining {

    private ShipmentPayNoteNodeChaining() {
    }

    public static Node specializeEur200ChfDhl(Node templateNode) {
        return DocTemplates.template(templateNode)
                .specialize(s -> s
                        .setName("Shipment Escrow — EUR 200 via DHL")
                        .setCurrency("EUR")
                        .setAmountTotal(20000)
                        .set(PayNotePaths.FUNDING_SOURCE_CURRENCY.pointer(), "CHF")
                        .bindChannel("shipmentCompanyChannel").accountId("acc_dhl_001"))
                .build();
    }

    public static Node instantiateAliceBob(Node specializedNode) {
        return DocTemplates.template(specializedNode)
                .instantiate(i -> i
                        .bindChannel("payerChannel").email("alice@gmail.com")
                        .bindChannel("payeeChannel").accountId("acc_bob_1234")
                        .bindChannel("guarantorChannel").accountId("acc_bank_1"))
                .build();
    }
}
