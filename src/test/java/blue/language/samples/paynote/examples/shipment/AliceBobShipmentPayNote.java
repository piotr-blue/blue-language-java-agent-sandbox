package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.samples.paynote.types.common.CommonTypes;

public final class AliceBobShipmentPayNote {

    private AliceBobShipmentPayNote() {
    }

    public static Node build(String timestamp) {
        Node instantiated = DHLShipmentPayNote.template(timestamp)
                .instantiate(i -> i
                        .bindChannel("payerChannel").email("alice@gmail.com")
                        .bindChannel("payeeChannel").accountId("acc_bob_1234")
                        .bindChannel("guarantorChannel").accountId("acc_bank_1"))
                .build();

        return DocTemplates.extend(instantiated, mutator -> mutator
                .putDocumentObject("extra", extra -> extra.put("createdBy", "AliceBobShipmentPayNote"))
                .participant("customerSupportChannel", "Customer support")
                .operation("cancel",
                        "payerChannel",
                        "Cancel before shipment confirmation",
                        steps -> steps
                                .emitType("CancellationRequested", CommonTypes.NamedEvent.class,
                                        payload -> payload.put("name", "Cancellation Requested"))
                                .capture().unlock()));
    }
}
