package blue.language.processor.harness;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeRef;
import blue.language.samples.paynote.examples.paynote.PayNoteCookbookExamplesV2;
import blue.language.samples.paynote.examples.shipment.ShipmentPayNote;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteProcessorExecutionTest {

    @Test
    void docTemplateShipmentChainCanBeExtendedWithExecutableTypedOperation() {
        Node shipmentBootstrap = ShipmentPayNote.template("2026-02-24T10:00:00Z")
                .specialize(s -> s
                        .setName("Shipment Escrow — Execution Test")
                        .setAmountTotal(80000))
                .instantiate(i -> i
                        .bindChannel("payerChannel").email("alice@example.com")
                        .bindChannel("payeeChannel").accountId("acc-merchant-1")
                        .bindChannel("guarantorChannel").accountId("acc-bank-1")
                        .bindChannel("shipmentCompanyChannel").accountId("acc-shipper-1"))
                .build();

        Node executableBootstrap = DocTemplates.extend(shipmentBootstrap, ext -> ext.operation(
                "adjustTotal",
                "payerChannel",
                Integer.class,
                "Adjust payable total in test flow.",
                steps -> steps.replaceExpression("ApplyAmount", "/amount/total", "event.message.request")));

        ProcessorSession session = new ProcessorHarness().start(executableBootstrap).initSession();
        session.callOperation("payerChannel", "adjustTotal", Integer.valueOf(90000));
        session.runUntilIdle();

        assertEquals(new BigInteger("90000"), session.document().getProperties().get("amount")
                .getProperties().get("total").getValue());
        assertTrue(containsEventType(session.emittedEvents(), PayNoteAliases.CAPTURE_LOCK_REQUESTED));
    }

    @Test
    void directChangeDslAddsContractsAndProcessorExecutesAnotherTypedOperation() {
        Node payNote = PayNotes.payNote("Direct Change Harness Execution")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(1000)
                .directChangeWithAllowList(
                        "directChange",
                        "payeeChannel",
                        "Allow constrained updates for note and tracking only.",
                        "/note",
                        "/shipping/trackingNumber")
                .operation("setTracking")
                    .channel("payeeChannel")
                    .requestType(Integer.class)
                    .description("Set tracking sequence")
                    .steps(steps -> steps.replaceExpression("SetTracking", "/shipping/trackingSequence", "event.message.request"))
                    .done()
                .buildDocument();

        ProcessorSession session = new ProcessorHarness().start(payNote).initSession();
        session.callOperation("payeeChannel", "setTracking", Integer.valueOf(42));
        session.callOperation("payeeChannel", "directChange", new Node().properties("changeset", new Node().items(
                new Node()
                        .properties("op", new Node().value("REPLACE"))
                        .properties("path", new Node().value("/note"))
                        .properties("val", new Node().value("note-updated-via-direct-change")))));
        session.runUntilIdle();

        assertEquals("Conversation/Change Operation",
                session.document().getProperties().get("contracts").getProperties()
                        .get("directChange").getType().getValue());
        assertEquals("Conversation/Change Workflow",
                session.document().getProperties().get("contracts").getProperties()
                        .get("directChangeImpl").getType().getValue());
        assertEquals("note-updated-via-direct-change", String.valueOf(session.document()
                .getProperties().get("note").getValue()));
        assertEquals(new BigInteger("42"), session.document().getProperties().get("shipping")
                .getProperties().get("trackingSequence").getValue());
    }

    @Test
    void jsVoucherBudgetFlowCapsCaptureRequestToRemainingAmount() {
        Node ticket25 = PayNoteCookbookExamplesV2.ticket25VoucherMonitoringBudgetJs();
        Node executable = DocTemplates.extend(ticket25, ext -> ext.operation(
                "reportTxn",
                "payeeChannel",
                Integer.class,
                "Report restaurant transaction amount.",
                steps -> steps.triggerEvent("EmitRestaurantTransaction", new Node()
                        .type(TypeRef.of(VoucherEvents.RestaurantTransactionReported.class).asTypeNode())
                        .properties("message", new Node()
                                .properties("amount", new Node().value(BlueDocDsl.expr("event.message.request")))))));

        ProcessorSession session = new ProcessorHarness().start(executable).initSession();
        session.callOperation("payeeChannel", "reportTxn", Integer.valueOf(12000));
        session.runUntilIdle();

        Node captureRequest = firstEventByType(session.emittedEvents(), PayNoteAliases.CAPTURE_FUNDS_REQUESTED);
        assertNotNull(captureRequest);
        assertEquals(new BigInteger("10000"), captureRequest.getProperties().get("amount").getValue());
    }

    @Test
    void donationRoundUpFlowRequestsRemainingCentsAfterFundsCapturedSignal() {
        Node ticket21 = PayNoteCookbookExamplesV2.ticket21DonationRoundUpJs();
        Node executable = DocTemplates.extend(ticket21, ext -> ext.operation(
                "confirmFundsCaptured",
                "guarantorChannel",
                Integer.class,
                "Inject guarantor captured signal.",
                steps -> steps.emitType(
                        "EmitFundsCaptured",
                        PayNoteTypes.FundsCaptured.class,
                        payload -> payload.put("amountCaptured", 3540))));

        ProcessorSession session = new ProcessorHarness().start(executable).initSession();
        session.callOperation("guarantorChannel", "confirmFundsCaptured", Integer.valueOf(1));
        session.runUntilIdle();

        Node captureRequest = firstEventByType(session.emittedEvents(), PayNoteAliases.CAPTURE_FUNDS_REQUESTED);
        assertNotNull(captureRequest);
        assertEquals(new BigInteger("60"), captureRequest.getProperties().get("amount").getValue());
    }

    private boolean containsEventType(List<Node> events, String expectedType) {
        return firstEventByType(events, expectedType) != null;
    }

    private Node firstEventByType(List<Node> events, String expectedType) {
        if (events == null) {
            return null;
        }
        for (Node event : events) {
            if (event == null) {
                continue;
            }
            String type = eventType(event);
            if (expectedType.equals(type)) {
                return event;
            }
        }
        return null;
    }

    private String eventType(Node event) {
        if (event == null) {
            return null;
        }
        if (event.getType() != null) {
            if (event.getType().getValue() != null) {
                return String.valueOf(event.getType().getValue());
            }
            if (event.getType().getBlueId() != null) {
                return event.getType().getBlueId();
            }
        }
        if (event.getProperties() != null && event.getProperties().get("type") != null) {
            Node typeNode = event.getProperties().get("type");
            if (typeNode.getValue() != null) {
                return String.valueOf(typeNode.getValue());
            }
            if (typeNode.getBlueId() != null) {
                return typeNode.getBlueId();
            }
            if (typeNode.getProperties() != null && typeNode.getProperties().get("blueId") != null) {
                Node blueIdNode = typeNode.getProperties().get("blueId");
                if (blueIdNode != null && blueIdNode.getValue() != null) {
                    return String.valueOf(blueIdNode.getValue());
                }
            }
        }
        return null;
    }
}
