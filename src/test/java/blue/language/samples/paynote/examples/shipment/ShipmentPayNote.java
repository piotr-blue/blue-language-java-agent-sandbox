package blue.language.samples.paynote.examples.shipment;

import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.sdk.CardTransaction;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNoteRole;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

public final class ShipmentPayNote {

    private ShipmentPayNote() {
    }

    public static DocTemplate template(String timestamp) {
        return PayNotes.payNote("iPhone Purchase — Shipment Escrow Template " + timestamp)
                .attach(CardTransaction.at(PayNotePaths.CARD_TXN_DETAILS))
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participants(p -> p.add(PayNoteRole.SHIPPER))
                .participantsUnion(ChannelKey.of("allParticipantsChannel"),
                        PayNoteRole.PAYER, PayNoteRole.PAYEE, PayNoteRole.GUARANTOR, PayNoteRole.SHIPPER)
                .cardCapture().lockOnInit()
                .operation("confirmShipment")
                    .channel(PayNoteRole.SHIPPER)
                    .description("Confirm that the shipment is complete.")
                    .steps(steps -> steps
                            .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", PayNoteRole.SHIPPER.channelKey().value()))
                            .emitType("RequestCardCaptureUnlock",
                                    PayNoteTypes.CardTransactionCaptureUnlockRequested.class,
                                    payload -> payload.putExpression("cardTransactionDetails",
                                            "document('" + PayNotePaths.CARD_TXN_DETAILS.pointer() + "')")))
                    .done()
                .cardCapture().guarantorConfirmCaptureLockedOp()
                .cardCapture().guarantorConfirmCaptureUnlockedOp()
                .directChangeWithAllowList("directChange",
                        PayNoteRole.PAYEE,
                        "Allow constrained updates for note and tracking only.",
                        "/note", "/shipping/trackingNumber")
                .onFundsCaptured("onFundsCaptured", steps -> steps
                        .replaceExpression("CopyCapturedAmount",
                                "/captureSummary/lastAmount",
                                "document('" + PayNotePaths.AMOUNT_TOTAL.pointer() + "')")
                        .emitType("CaptureAudit", ShippingEvents.DeliveryReported.class, payload -> payload
                                .putExpression("shipmentId", "document('/shipment/id')")
                                .put("status", "captured")))
                .template();
    }
}
