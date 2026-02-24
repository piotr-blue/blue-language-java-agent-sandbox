package blue.language.samples.paynote.examples.shipment;

import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.ShippingEvents;

public final class ShipmentPayNote {

    private ShipmentPayNote() {
    }

    public static DocTemplate template(String timestamp) {
        return PayNotes.payNote("iPhone Purchase — Shipment Escrow Template " + timestamp)
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participant("shipmentCompanyChannel", "Shipment company confirms delivery")
                .participantsUnion("allParticipantsChannel",
                        "payerChannel", "payeeChannel", "guarantorChannel", "shipmentCompanyChannel")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("confirmShipment", op -> op
                            .channel("shipmentCompanyChannel")
                            .description("Confirm that the shipment is complete.")
                            .steps(steps -> steps
                                    .emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                            payload -> payload.put("source", "shipmentCompanyChannel"))))
                    .done()
                .directChangeWithAllowList("directChange",
                        "payeeChannel",
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
