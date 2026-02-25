package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.DocBuilder;

public final class DHLShipmentPayNote {

    private DHLShipmentPayNote() {
    }

    public static Node template(String timestamp) {
        return DocBuilder.edit(ShipmentPayNote.template(timestamp))
                .withName("Shipment Escrow — EUR 200 via DHL")
                .set("/currency", "EUR")
                .set("/amount/total", 20000)
                .set(PayNotePaths.FUNDING_SOURCE_CURRENCY.pointer(), "CHF")
                .buildDocument();
    }
}
