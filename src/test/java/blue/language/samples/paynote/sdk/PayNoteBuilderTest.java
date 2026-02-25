package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.dsl.MyOsTimeline;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.dsl.TypeRef;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteBuilderTest {

    @Test
    void convertsMajorAmountsUsingCurrencyScale() {
        Node usd = PayNotes.payNote("USD")
                .currency(IsoCurrency.USD)
                .amountTotalMajor(new BigDecimal("4.25"))
                .buildDocument();
        assertEquals(425L, usd.getAsInteger("/amount/total/value").longValue());

        Node jpy = PayNotes.payNote("JPY")
                .currency(IsoCurrency.JPY)
                .amountTotalMajor(new BigDecimal("125"))
                .buildDocument();
        assertEquals(125L, jpy.getAsInteger("/amount/total/value").longValue());

        IllegalArgumentException invalidScale = assertThrows(IllegalArgumentException.class, () ->
                PayNotes.payNote("Invalid")
                        .currency(IsoCurrency.USD)
                        .amountTotalMajor(new BigDecimal("4.522")));
        assertTrue(invalidScale.getMessage().contains("exceeds currency fraction digits"));
    }

    @Test
    void keepsPayNoteTransactionAgnosticAndImplicitCoreParticipants() {
        Node document = PayNotes.payNote("iPhone Purchase")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participant("shipmentCompanyChannel", "Shipment company")
                .participantsUnion(ChannelKey.of("allParticipantsChannel"),
                        "payerChannel", "payeeChannel", "guarantorChannel", "shipmentCompanyChannel")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("confirmShipment", op -> op
                            .channel("shipmentCompanyChannel")
                            .description("Confirm shipment")
                            .requestType(String.class)
                            .steps(steps -> steps.emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", "shipmentCompanyChannel"))))
                    .done()
                .buildDocument();

        PayNoteAssert.assertThat(document)
                .isPayNoteDocument()
                .hasParticipant("payerChannel")
                .hasParticipant("payeeChannel")
                .hasParticipant("guarantorChannel")
                .hasParticipant("shipmentCompanyChannel")
                .captureLocksOnInit()
                .captureUnlocksViaOperation("confirmShipment");
        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                document.getAsText("/contracts/allParticipantsChannel/type/value"));
    }

    @Test
    void captureLockFailsFastWhenNoUnlockConfigured() {
        IllegalStateException missingUnlock = assertThrows(IllegalStateException.class, () ->
                PayNotes.payNote("Missing unlock")
                        .currency(IsoCurrency.USD)
                        .amountTotalMinor(500)
                        .capture().lockOnInit()
                        .done()
                        .buildDocument());
        assertTrue(missingUnlock.getMessage().contains("resolution path"));

        Node complete = PayNotes.payNote("Complete lock plan")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(500)
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(ShippingEvents.ShipmentConfirmed.class)
                    .done()
                .buildDocument();
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                complete.getAsText("/contracts/unlockCaptureWhenShipmentConfirmed/steps/0/event/type/value"));
    }

    @Test
    void distinguishesUnlockAndRequestCaptureSemantics() {
        Node externalUnlock = PayNotes.payNote("External unlock")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("confirm", op -> op
                            .channel("payerChannel")
                            .description("External unlock"))
                    .done()
                .buildDocument();
        PayNoteAssert.assertThat(externalUnlock)
                .captureLocksOnInit()
                .captureUnlocksViaOperation("confirm");

        Node captureRequest = PayNotes.payNote("Capture request")
                .capture()
                    .lockOnInit()
                    .requestOnOperation("confirm", op -> op
                            .channel("payerChannel")
                            .description("Request capture"))
                    .done()
                .buildDocument();
        PayNoteAssert.assertThat(captureRequest)
                .captureLocksOnInit()
                .captureRequestsViaOperation("confirm");
    }

    @Test
    void separatesDocumentDefinitionFromMyOsBindings() {
        Node document = PayNotes.payNote("Binding separation")
                .currency(IsoCurrency.EUR)
                .amountTotalMinor(20000)
                .buildDocument();
        assertThrows(IllegalArgumentException.class, () -> document.getAsText("/channelBindings/payerChannel/email/value"));

        Node bootstrap = PayNotes.payNote("Binding separation")
                .currency(IsoCurrency.EUR)
                .amountTotalMinor(20000)
                .bootstrap()
                .bind("payerChannel").email("alice@gmail.com")
                .bind("payeeChannel").accountId("acc_bob_1234")
                .bind("guarantorChannel").accountId("acc_bank_1")
                .build();
        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals("alice@gmail.com", bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
    }

    @Test
    void allowsPayNoteWithoutCurrencyOrAmountAndSupportsReferenceTransactionMetadata() {
        Node document = PayNotes.payNote("Metadata only")
                .referenceTransactionPath("/referenceTransaction")
                .buildDocument();

        assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"));
        assertEquals("/referenceTransaction", document.getAsText("/referenceTransactionPath/value"));
        assertThrows(IllegalArgumentException.class, () -> document.getAsText("/amount/total/value"));
    }

    @Test
    void participantOverrideMustRemainTypeCompatible() {
        Node compatible = PayNotes.payNote("Compatible override")
                .participant("shipmentCompanyChannel", "DHL", MyOsTimeline.accountId("acc_dhl_001").asNode())
                .buildDocument();
        assertEquals(TypeAliases.MYOS_TIMELINE_CHANNEL,
                compatible.getAsText("/contracts/shipmentCompanyChannel/type/value"));

        IllegalArgumentException incompatible = assertThrows(IllegalArgumentException.class, () ->
                PayNotes.payNote("Incompatible")
                        .participant("payerChannel", "bad", new Node().type("Conversation/Operation")));
        assertTrue(incompatible.getMessage().contains("type-compatible"));
    }

    @Test
    void supportsReserveAndRefundOperationSymmetryHelpers() {
        Node document = PayNotes.payNote("Reserve and refund helpers")
                .reserveLockedUntilOperation("approveReserve",
                        "payerChannel",
                        "Approve reserve",
                        ShippingEvents.ShipmentConfirmed.class)
                .refundLockedUntilOperation("approveRefund",
                        "payeeChannel",
                        "Approve refund",
                        ShippingEvents.DeliveryReported.class)
                .reserveLockedUntilEvent(ShippingEvents.ShipmentConfirmed.class)
                .refundLockedUntilEvent(ShippingEvents.DeliveryReported.class)
                .buildDocument();

        assertEquals(PayNoteAliases.RESERVE_FUNDS_REQUESTED,
                document.getAsText("/contracts/approveReserveImpl/steps/1/event/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                document.getAsText("/contracts/approveRefundImpl/steps/1/event/type/value"));
        assertEquals(PayNoteAliases.RESERVE_FUNDS_REQUESTED,
                document.getAsText("/contracts/reserveWhenShipmentConfirmed/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                document.getAsText("/contracts/refundWhenDeliveryReported/steps/0/event/type/value"));
    }

    @Test
    void acceptsEventsFromGeneratesParticipantIngressOperation() {
        Node document = PayNotes.payNote("Ingress")
                .acceptsEventsFrom("inspector")
                .buildDocument();

        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/inspectorChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/inspectorEmitEvents/type/value"));
        assertEquals("inspectorChannel", document.getAsText("/contracts/inspectorEmitEvents/channel/value"));
        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION,
                document.getAsText("/contracts/inspectorEmitEventsImpl/type/value"));
        String js = document.getAsText("/contracts/inspectorEmitEventsImpl/steps/0/code/value");
        assertTrue(js.contains("Array.isArray(req) ? req : [req]"));
        assertTrue(!js.contains("const allowed = ["));
    }

    @Test
    void acceptsEventsFromSupportsAllowedEventTypeFiltering() {
        Node document = PayNotes.payNote("Filtered ingress")
                .acceptsEventsFrom("guarantor",
                        PayNoteTypes.FundsReserved.class,
                        ShippingEvents.ShipmentConfirmed.class)
                .buildDocument();

        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/guarantorEmitEvents/type/value"));
        String js = document.getAsText("/contracts/guarantorEmitEventsImpl/steps/0/code/value");
        assertTrue(js.contains("const allowed = ["));
        assertTrue(js.contains(TypeRef.of(PayNoteTypes.FundsReserved.class).alias()));
        assertTrue(js.contains(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED));
        assertTrue(js.contains("allowed.includes(e.type)"));
    }

    @Test
    void participantLabelsAreMergedAcrossMultipleParticipants() {
        Node document = PayNotes.payNote("Labels merged")
                .participant("shipmentCompanyChannel", "Shipment company")
                .participant("inspectorChannel", "Inspector")
                .buildDocument();

        assertEquals("Shipment company",
                document.getAsText("/participantLabels/shipmentCompanyChannel/value"));
        assertEquals("Inspector",
                document.getAsText("/participantLabels/inspectorChannel/value"));
    }
}
