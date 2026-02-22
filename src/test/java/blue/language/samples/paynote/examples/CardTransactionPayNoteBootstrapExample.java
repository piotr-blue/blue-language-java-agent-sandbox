package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.domain.ShippingEvents;

import java.util.Arrays;

public final class CardTransactionPayNoteBootstrapExample {

    private CardTransactionPayNoteBootstrapExample() {
    }

    public static Node build(String timestamp,
                             String payerAccountId,
                             String payeeAccountId,
                             String guarantorAccountId,
                             String shipmentCompanyAccountId) {
        return MyOsDsl.bootstrap()
                .documentName("iPhone Purchase - " + timestamp)
                .documentType(PayNoteAliases.CARD_TRANSACTION_PAYNOTE)
                .putDocumentValue("currency", "USD")
                .putDocumentObject("amount", amount -> amount.put("total", 80000))
                .putDocumentObject("cardTransactionDetails", details -> details
                        .put("authorizationId", "AUTH_123")
                        .put("merchantId", "MERCHANT_456")
                        .put("cardNetwork", "VISA")
                        .put("captureAmountMinor", 80000))
                .putDocumentObject("payNoteInitialStateDescription", description -> description
                        .put("summary",
                                "This is a protected payment of $800.00 USD reserved by guarantor and released only after shipment confirmation.")
                        .put("details",
                                "Participants: payer, payee, guarantor, shipment company.\n" +
                                        "confirmShipment triggers card capture unlock request."))
                .contracts(c -> {
                    c.timelineChannel("payerChannel");
                    c.timelineChannel("payeeChannel");
                    c.timelineChannel("guarantorChannel");
                    c.timelineChannel("shipmentCompanyChannel");

                    c.putRaw("allParticipantsChannel", new Node()
                            .type(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL)
                            .properties("channels", new Node().items(Arrays.asList(
                                    new Node().value("payerChannel"),
                                    new Node().value("payeeChannel"),
                                    new Node().value("guarantorChannel"),
                                    new Node().value("shipmentCompanyChannel")))));

                    c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
                    c.onLifecycle("bootstrap", "initLifecycleChannel", steps -> steps
                            .js("RequestCardTransactionCaptureLock", requestCaptureLockProgram()));

                    c.operation("confirmShipment", "shipmentCompanyChannel", "Confirm that delivery is complete.");
                    c.implementOperation("confirmShipmentImpl", "confirmShipment", steps -> steps
                            .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", "shipmentCompanyChannel"))
                            .js("RequestCardTransactionCaptureUnlock", requestCaptureUnlockProgram()));

                    c.operation("confirmCardTransactionCaptureLocked",
                            "guarantorChannel",
                            "Confirm that card transaction capture is locked.");
                    c.implementOperation("confirmCardTransactionCaptureLockedImpl",
                            "confirmCardTransactionCaptureLocked",
                            steps -> steps.js("EmitCaptureLocked", emitSimpleEventProgram(
                                    PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCKED)));

                    c.operation("confirmCardTransactionCaptureUnlocked",
                            "guarantorChannel",
                            "Confirm that card transaction capture is unlocked.");
                    c.implementOperation("confirmCardTransactionCaptureUnlockedImpl",
                            "confirmCardTransactionCaptureUnlocked",
                            steps -> steps.js("EmitCaptureUnlocked", emitSimpleEventProgram(
                                    PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCKED)));

                    c.operation("directChange", "allParticipantsChannel", "Allow constrained direct document update.");
                    c.implementOperation("directChangeImpl", "directChange", steps -> steps
                            .js("PrepareConstrainedChangeset", directChangeProgram())
                            .updateDocumentFromExpression("ApplyConstrainedChangeset", "steps.PrepareConstrainedChangeset.changeset"));
                })
                .policies(p -> p
                        .contractsChangePolicy("allow-listed-direct-change", "directChange operation is constrained")
                        .changesetAllowList("directChange", "/note", "/shipping/trackingNumber")
                        .operationRateLimit("directChange", 5, "PT5M"))
                .bindAccount("payerChannel", payerAccountId)
                .bindAccount("payeeChannel", payeeAccountId)
                .bindAccount("guarantorChannel", guarantorAccountId)
                .bindAccount("shipmentCompanyChannel", shipmentCompanyAccountId)
                .build();
    }

    private static JsProgram requestCaptureLockProgram() {
        return BlueDocDsl.js(js -> js.returnOutput(
                JsOutputBuilder.output().eventsArray(
                        JsArrayBuilder.array().itemObject(
                                JsObjectBuilder.object()
                                        .propString("type", PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED)
                                        .propRaw("cardTransactionDetails", "document('/cardTransactionDetails')")))));
    }

    private static JsProgram requestCaptureUnlockProgram() {
        return BlueDocDsl.js(js -> js.returnOutput(
                JsOutputBuilder.output().eventsArray(
                        JsArrayBuilder.array().itemObject(
                                JsObjectBuilder.object()
                                        .propString("type", PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED)
                                        .propRaw("cardTransactionDetails", "document('/cardTransactionDetails')")))));
    }

    private static JsProgram emitSimpleEventProgram(String type) {
        return BlueDocDsl.js(js -> js.returnOutput(
                JsOutputBuilder.output().eventsArray(
                        JsArrayBuilder.array().itemObject(
                                JsObjectBuilder.object().propString("type", type)))));
    }

    private static JsProgram directChangeProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("request", "event.message?.request ?? {}")
                .constVar("changeset", "request.changeset ?? []")
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw("changeset")
                        .eventsArray(JsArrayBuilder.array())));
    }
}
