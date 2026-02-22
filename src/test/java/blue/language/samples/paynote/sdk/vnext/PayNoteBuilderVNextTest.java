package blue.language.samples.paynote.sdk.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteBuilderVNextTest {

    @Test
    void buildsCardShipmentEscrowWithTypedHooksAndRoleBindings() {
        Node bootstrap = PayNotes.cardTransaction("iPhone Purchase")
                .currency("USD")
                .amountTotal(80000)
                .participants(p -> p
                        .payer()
                        .payee()
                        .guarantor()
                        .shipper("shipmentCompanyChannel"))
                .compositeParticipants("allParticipantsChannel",
                        "payerChannel", "payeeChannel", "guarantorChannel", "shipmentCompanyChannel")
                .lockCardCaptureOnInit("/cardTransactionDetails")
                .operation("confirmShipment",
                        "shipmentCompanyChannel",
                        "Confirm that shipment is complete.",
                        steps -> steps.emitType("ShipmentConfirmed",
                                ShippingEvents.ShipmentConfirmed.class,
                                payload -> payload.put("source", "shipmentCompanyChannel")))
                .unlockCardCaptureWhen(ShippingEvents.ShipmentConfirmed.class, "/cardTransactionDetails")
                .confirmLockOperation("guarantorChannel")
                .confirmUnlockOperation("guarantorChannel")
                .onFundsCaptured("onFundsCapturedHook", steps -> steps
                        .replaceExpression("CopyCapturedAmount", "/captureSummary/lastAmount", "document('/amount/total')")
                        .emitType("EmitCaptureAudit",
                                ShippingEvents.DeliveryReported.class,
                                payload -> payload
                                        .putExpression("shipmentId", "document('/shipment/id')")
                                        .put("status", "captured")))
                .directChangeWithAllowList("directChange",
                        "payeeChannel",
                        "Allow direct updates for shipment note and tracking only.",
                        "/note", "/shipping/trackingNumber")
                .bindRoleEmail("payer", "alice@gmail.com")
                .bindRoleAccount("payee", "acc_bob_1234")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .bindRoleAccount("shipmentCompany", "acc_dhl_001")
                .build();

        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_PAYNOTE, bootstrap.getAsText("/document/type/value"));
        assertEquals("USD", bootstrap.getAsText("/document/currency/value"));
        assertEquals(80000, bootstrap.getAsInteger("/document/amount/total/value").intValue());

        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/allParticipantsChannel/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                bootstrap.getAsText("/document/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                bootstrap.getAsText("/document/contracts/unlockCardCaptureWhenEvent/event/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED,
                bootstrap.getAsText("/document/contracts/onInitLockCardCapture/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED,
                bootstrap.getAsText("/document/contracts/unlockCardCaptureWhenEvent/steps/0/event/type/value"));

        assertEquals("${document('/amount/total')}",
                bootstrap.getAsText("/document/contracts/onFundsCapturedHook/steps/0/changeset/0/val/value"));
        assertEquals(TypeAliases.SHIPPING_DELIVERY_REPORTED,
                bootstrap.getAsText("/document/contracts/onFundsCapturedHook/steps/1/event/type/value"));

        String directChangeCode = bootstrap.getAsText("/document/contracts/directChangeImpl/steps/0/code/value");
        assertTrue(directChangeCode.contains("request.changeset ?? []"));
        assertEquals("/shipping/trackingNumber",
                bootstrap.getAsText("/document/policies/changesetAllowList/directChange/1/value"));
        assertEquals("alice@gmail.com", bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("acc_dhl_001", bootstrap.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));
    }
}
