package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.sdk.CardTransaction;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNoteRole;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.domain.ShippingEvents;

public final class PayNoteComplexityLadderExamples {

    private PayNoteComplexityLadderExamples() {
    }

    // Step 1 (tiny useful paynotes, mostly one-liners)
    public static Node simpleCardLock() {
        return PayNotes.payNote("Simple Card Lock")
                .attach(CardTransaction.defaultRef())
                .currency(IsoCurrency.USD)
                .amountTotalMajor("12.00")
                .cardCapture().lockOnInit()
                .buildDocument();
    }

    public static Node simpleReserveAndCapture() {
        return PayNotes.payNote("Simple Reserve + Capture")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("19.99")
                .reserveAndCaptureImmediatelyOnInit()
                .buildDocument();
    }

    public static Node simpleRefundOperation() {
        return PayNotes.payNote("Simple Refund")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("9.50")
                .refundFullOperation(PayNoteRole.PAYEE)
                .buildDocument();
    }

    // Step 2 (custom operations + workflow composition)
    public static Node mediumShipmentEscrow() {
        return PayNotes.payNote("Medium Shipment Escrow")
                .attach(CardTransaction.defaultRef())
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participants(p -> p.shipper())
                .cardCapture().lockOnInit()
                .operation("confirmShipment")
                    .channel(PayNoteRole.SHIPPER)
                    .description("Shipment company confirms shipment.")
                    .steps(steps -> steps
                            .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", "shipmentCompanyChannel")))
                    .done()
                .cardCapture().unlockWhen(ShippingEvents.ShipmentConfirmed.class)
                .cardCapture().guarantorConfirmCaptureLockedOp()
                .cardCapture().guarantorConfirmCaptureUnlockedOp()
                .directChangeWithAllowList("directChange",
                        PayNoteRole.PAYEE,
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
                    .channel(PayNoteRole.GUARANTOR)
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
