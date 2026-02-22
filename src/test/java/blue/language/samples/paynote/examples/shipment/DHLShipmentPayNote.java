package blue.language.samples.paynote.examples.shipment;

import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.sdk.vnext.PayNoteRole;

public final class DHLShipmentPayNote {

    private DHLShipmentPayNote() {
    }

    public static DocTemplate template(String timestamp) {
        return ShipmentPayNote.template(timestamp)
                .specialize(s -> s
                        .setName("Shipment Escrow — EUR 200 via DHL")
                        .setCurrency("EUR")
                        .setAmountTotal(20000)
                        .set(PayNotePaths.FUNDING_SOURCE_CURRENCY.pointer(), "CHF")
                        .bindRole(PayNoteRole.SHIPPER.roleKey()).accountId("acc_dhl_001"));
    }
}
