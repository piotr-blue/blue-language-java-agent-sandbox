package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.processor.model.JsonPatch;
import blue.language.samples.paynote.dsl.PayNoteAliases;

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
                .applyPatch(JsonPatch.add("/document/contracts/customerSupportChannel",
                        new Node().type("Conversation/Timeline Channel")))
                .applyPatch(JsonPatch.add("/document/contracts/cancel",
                        new Node()
                                .type("Conversation/Operation")
                                .properties("channel", new Node().value("payerChannel"))
                                .properties("description", new Node().value("Cancel before shipment confirmation"))))
                .applyPatch(JsonPatch.add("/document/contracts/cancelImpl",
                        new Node()
                                .type("Conversation/Sequential Workflow Operation")
                                .properties("operation", new Node().value("cancel"))
                                .properties("steps", new Node().items(
                                        new Node()
                                                .type("Conversation/Trigger Event")
                                                .properties("event", new Node()
                                                        .type("Common/Named Event")
                                                        .properties("name", new Node().value("Cancellation Requested"))),
                                        new Node()
                                                .type("Conversation/Trigger Event")
                                                .properties("event", new Node()
                                                        .type(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED))
                                )))));
    }
}
