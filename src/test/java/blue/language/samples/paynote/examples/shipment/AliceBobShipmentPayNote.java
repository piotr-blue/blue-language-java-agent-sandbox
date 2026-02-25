package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.sdk.DocBuilder;
import blue.language.samples.paynote.types.common.CommonTypes;

public final class AliceBobShipmentPayNote {

    private AliceBobShipmentPayNote() {
    }

    public static Node build(String timestamp) {
        Node document = DocBuilder.edit(DHLShipmentPayNote.template(timestamp))
                .set("/extra/createdBy", "AliceBobShipmentPayNote")
                .participant("customerSupportChannel", "Customer support")
                .operation("cancel",
                        "payerChannel",
                        "Cancel before shipment confirmation",
                        steps -> steps
                                .emitType("CancellationRequested", CommonTypes.NamedEvent.class,
                                        payload -> payload.put("name", "Cancellation Requested"))
                                .capture().unlock())
                .buildDocument();

        return MyOsDsl.bootstrap(document)
                .bind("shipmentCompanyChannel").accountId("acc_dhl_001")
                .bind("payerChannel").email("alice@gmail.com")
                .bind("payeeChannel").accountId("acc_bob_1234")
                .bind("guarantorChannel").accountId("acc_bank_1")
                .build();
    }
}
