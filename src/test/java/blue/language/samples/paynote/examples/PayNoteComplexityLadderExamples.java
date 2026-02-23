package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.domain.ShippingEvents;

public final class PayNoteComplexityLadderExamples {

    private PayNoteComplexityLadderExamples() {
    }

    // Step 1 (tiny useful paynotes, all complete lock->unlock or direct useful flow)
    public static Node tinyCaptureAfterShipmentOp() {
        return PayNotes.payNote("Tiny Capture After Shipment")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("12.00")
                .captureLockedUntilOperation("confirmShipment",
                        "shipmentCompanyChannel",
                        "Shipment company confirms delivery",
                        ShippingEvents.ShipmentConfirmed.class)
                .buildDocument();
    }

    public static Node tinyCaptureAfterBuyerApprovalOp() {
        return PayNotes.payNote("Tiny Capture After Buyer Approval")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("19.99")
                .capture()
                    .lockOnInit()
                    .unlockExternalOnOperation("approveCapture", op -> op
                            .channel("payerChannel")
                            .description("Buyer approves capture.")
                            .requestType(String.class))
                    .done()
                .buildDocument();
    }

    public static Node tinyCaptureAfterTrackingChange() {
        return PayNotes.payNote("Tiny Capture After Tracking Change")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("15.00")
                .captureLockedUntilDocPathChanges("/shipping/trackingNumber")
                .buildDocument();
    }

    public static Node tinyCaptureAfterEvent() {
        return PayNotes.payNote("Tiny Capture After Event")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("22.50")
                .captureLockedUntilEvent(ShippingEvents.DeliveryReported.class)
                .buildDocument();
    }

    public static Node tinyReserveThenCaptureOnEvent() {
        return PayNotes.payNote("Tiny Reserve Then Capture")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("8.75")
                .reserveOnInit()
                .captureOnEvent(ShippingEvents.ShipmentConfirmed.class, "captureWhenShipmentConfirmed")
                .buildDocument();
    }

    public static Node tinyRefundOperation() {
        return PayNotes.payNote("Tiny Refund")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("9.50")
                .refundOnOperation("requestRefund", "payeeChannel", "Payee requests refund.")
                .buildDocument();
    }

    public static Node tinyReleaseOperation() {
        return PayNotes.payNote("Tiny Release")
                .currency(IsoCurrency.CHF)
                .amountTotalMajor("11.00")
                .releaseReservationOperation("releaseReservation", "guarantorChannel")
                .buildDocument();
    }

    public static Node tinyCancellationOperation() {
        return PayNotes.payNote("Tiny Cancel")
                .currency(IsoCurrency.USD)
                .requestCancellationOperation("payerChannel")
                .buildDocument();
    }

    // Step 2 (custom operations + workflow composition)
    public static Node mediumShipmentEscrow() {
        return PayNotes.payNote("Medium Shipment Escrow")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participant("shipmentCompanyChannel", "Shipment company confirms shipment.")
                .participantsUnion("allParticipantsChannel",
                        "payerChannel", "payeeChannel", "guarantorChannel", "shipmentCompanyChannel")
                .capture()
                    .lockOnInit()
                    .unlockExternalOnOperation("confirmShipment", op -> op
                            .channel("shipmentCompanyChannel")
                            .description("Shipment company confirms shipment.")
                            .steps(steps -> steps
                                    .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                            payload -> payload.put("source", "shipmentCompanyChannel"))
                                    .replaceValue("SetShipmentConfirmedAt", "/signals/shipmentConfirmedAt", "confirmed")))
                    .done()
                .directChangeWithAllowList("directChange",
                        "payeeChannel",
                        "Allow only safe payee edits",
                        "/note", "/shipping/trackingNumber")
                .buildDocument();
    }

    // Step 3 (single JS step with 100+ lines)
    public static Node hugeJsRiskReview() {
        return PayNotes.payNote("Huge JS Risk Review")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("125.00")
                .operation("riskReview")
                    .channel("guarantorChannel")
                    .description("Run heavy deterministic normalization and scoring.")
                    .steps(steps -> steps
                            .js("RiskEngine", hugeRiskProgram())
                            .updateDocumentFromExpression("ApplyRiskPatch", "steps.RiskEngine.changeset")
                            .emitType("RiskReviewCompleted", CommonTypes.NamedEvent.class, payload -> payload
                                    .put("name", "RiskReviewCompleted")))
                    .done()
                .buildDocument();
    }

    private static JsProgram hugeRiskProgram() {
        JsProgram.Builder builder = JsProgram.builder();
        builder.readRequest("request");
        builder.line("const source = request.payload ?? {};");
        for (int i = 0; i < 105; i++) {
            builder.line("const factor" + i + " = Number(source.factor" + i + " ?? 0);");
        }
        builder.line("const aggregateScore = [");
        for (int i = 0; i < 105; i++) {
            builder.line("  factor" + i + ",");
        }
        builder.line("].reduce((sum, value) => sum + value, 0);");
        builder.returnOutput(JsOutputBuilder.output()
                .changesetRaw(JsPatchBuilder.patch()
                        .replaceExpression("/risk/aggregateScore", "aggregateScore")
                        .replaceExpression("/risk/lastRunAtStep", "'deterministic'")
                        .build())
                .eventsArray(JsArrayBuilder.array().itemRaw(JsObjectBuilder.object()
                        .propString("type", "Common/Named Event")
                        .propString("name", "RiskScoreComputed")
                        .build())));
        return builder.build();
    }
}
