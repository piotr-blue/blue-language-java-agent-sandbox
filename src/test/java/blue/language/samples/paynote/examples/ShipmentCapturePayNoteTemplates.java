package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.sdk.DocBuilder;
import blue.language.samples.paynote.sdk.SimpleDocBuilder;
import blue.language.samples.paynote.types.domain.ShippingEvents;

public final class ShipmentCapturePayNoteTemplates {

    private ShipmentCapturePayNoteTemplates() {
    }

    public static Node captureOnShipmentTemplate(String timestamp) {
        return SimpleDocBuilder.name("Shipment Escrow Template - " + timestamp)
                .type(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE)
                .set("/currency", "EUR")
                .set("/amount/total", 0)
                .set("/funding/sourceCurrency", "EUR")
                .participants("payerChannel", "payeeChannel", "guarantorChannel", "shipmentCompanyChannel")
                .onInit("onInitReserveFunds", steps -> steps.js("ReserveFunds", reserveFundsProgram()))
                .operation("confirmShipment",
                        "shipmentCompanyChannel",
                        "Confirm delivery; triggers capture.",
                        steps -> steps
                                .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                        payload -> payload.put("source", "shipmentCompanyChannel"))
                                .js("CaptureFunds", captureFundsProgram()))
                .buildDocument();
    }

    public static Node shipmentEscrowTemplate(String timestamp) {
        return captureOnShipmentTemplate(timestamp);
    }

    public static Node eur200FromChfWithDhl(Node baseTemplate) {
        return DocBuilder.edit(baseTemplate)
                .withName("Shipment Escrow — EUR 200 via DHL")
                .set("/currency", "EUR")
                .set("/amount/total", 20000)
                .set("/funding/sourceCurrency", "CHF")
                .buildDocument();
    }

    public static Node instantiateForAliceBob(Node specializedTemplate,
                                              String dhlAccountId,
                                              String aliceAccountId,
                                              String bobAccountId,
                                              String bankAccountId) {
        return MyOsDsl.bootstrap(specializedTemplate)
                .bind("shipmentCompanyChannel").accountId(dhlAccountId)
                .bind("payerChannel").accountId(aliceAccountId)
                .bind("payeeChannel").accountId(bobAccountId)
                .bind("guarantorChannel").accountId(bankAccountId)
                .build();
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
