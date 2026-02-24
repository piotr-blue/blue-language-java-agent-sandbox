package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.domain.ShippingEvents;

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
                            .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", "shipmentCompanyChannel"))
                            .js("CaptureFunds", captureFundsProgram()));
                })
                .build();
    }

    public static DocTemplate shipmentEscrowTemplate(String timestamp) {
        return DocTemplates.template(captureOnShipmentTemplate(timestamp));
    }

    public static DocTemplate eur200FromChfWithDhl(DocTemplate baseTemplate, String dhlAccountId) {
        return baseTemplate.specialize(s -> s
                .setCurrency("EUR")
                .setAmountTotal(20000)
                .set("/funding/sourceCurrency", "CHF")
                .bindRole("shipmentCompany").accountId(dhlAccountId));
    }

    public static Node instantiateForAliceBob(DocTemplate specializedTemplate,
                                              String aliceAccountId,
                                              String bobAccountId,
                                              String bankAccountId) {
        return specializedTemplate.instantiate(i -> i
                .bindRole("payer").accountId(aliceAccountId)
                .bindRole("payee").accountId(bobAccountId)
                .bindRole("guarantor").accountId(bankAccountId))
                .build();
    }

    public static Node eur200FromChfWithDhl(Node baseTemplate, String dhlAccountId) {
        return eur200FromChfWithDhl(DocTemplates.template(baseTemplate), dhlAccountId).build();
    }

    public static Node instantiateForAliceBob(Node specializedTemplate,
                                              String aliceAccountId,
                                              String bobAccountId,
                                              String bankAccountId) {
        return instantiateForAliceBob(DocTemplates.template(specializedTemplate), aliceAccountId, bobAccountId, bankAccountId);
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
