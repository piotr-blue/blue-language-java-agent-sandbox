package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;

import java.util.Arrays;

public final class ShipmentCapturePayNoteTemplates {

    private ShipmentCapturePayNoteTemplates() {
    }

    public static Node captureOnShipmentTemplate(String timestamp) {
        return MyOsDsl.bootstrap()
                .documentName("Shipment Escrow Template - " + timestamp)
                .documentType(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE)
                .putDocumentValue("currency", "EUR")
                .putDocumentObject("amount", amount -> amount.put("total", 0))
                .putDocumentObject("funding", funding -> funding.put("sourceCurrency", "EUR"))
                .contracts(c -> {
                    c.timelineChannel("payerChannel");
                    c.timelineChannel("payeeChannel");
                    c.timelineChannel("guarantorChannel");
                    c.timelineChannel("shipmentCompanyChannel");

                    c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
                    c.onLifecycle("onInitReserveFunds", "initLifecycleChannel", steps -> steps
                            .js("ReserveFunds", reserveFundsProgram()));

                    c.operation("confirmShipment", "shipmentCompanyChannel", "Confirm delivery; triggers capture.");
                    c.implementOperation("confirmShipmentImpl", "confirmShipment", steps -> steps
                            .triggerEvent("ShipmentConfirmed", shipmentConfirmedEvent())
                            .js("CaptureFunds", captureFundsProgram()));
                })
                .build();
    }

    public static Node eur200FromChfWithDhl(Node baseTemplate, String dhlAccountId) {
        Node specialized = DocTemplates.applyPatch(baseTemplate, Arrays.asList(
                JsonPatch.replace("/document/currency", new Node().value("EUR")),
                JsonPatch.replace("/document/amount/total", new Node().value(20000)),
                JsonPatch.replace("/document/funding/sourceCurrency", new Node().value("CHF"))
        ));

        return DocTemplates.extend(specialized, mutator -> mutator.bindAccount("shipmentCompanyChannel", dhlAccountId));
    }

    public static Node instantiateForAliceBob(Node specializedTemplate,
                                              String aliceAccountId,
                                              String bobAccountId,
                                              String bankAccountId) {
        return DocTemplates.extend(specializedTemplate, mutator -> mutator
                .bindAccount("payerChannel", aliceAccountId)
                .bindAccount("payeeChannel", bobAccountId)
                .bindAccount("guarantorChannel", bankAccountId));
    }

    private static Node shipmentConfirmedEvent() {
        return new Node()
                .type(TypeAliases.CONVERSATION_EVENT)
                .properties("kind", new Node().value("Shipment Confirmed"));
    }

    private static JsProgram reserveFundsProgram() {
        return BlueDocDsl.js(js -> js.returnOutput(
                JsOutputBuilder.output().eventsArray(
                        JsArrayBuilder.array().itemObject(
                                JsObjectBuilder.object()
                                        .propString("type", PayNoteAliases.RESERVE_FUNDS_REQUESTED)
                                        .propRaw("amount", "document('/amount/total')")
                                        .propRaw("currency", "document('/currency')")
                                        .propRaw("sourceCurrency", "document('/funding/sourceCurrency')")))));
    }

    private static JsProgram captureFundsProgram() {
        return BlueDocDsl.js(js -> js.returnOutput(
                JsOutputBuilder.output().eventsArray(
                        JsArrayBuilder.array().itemObject(
                                JsObjectBuilder.object()
                                        .propString("type", PayNoteAliases.CAPTURE_FUNDS_REQUESTED)
                                        .propRaw("amount", "document('/amount/total')")
                                        .propRaw("currency", "document('/currency')")))));
    }
}
