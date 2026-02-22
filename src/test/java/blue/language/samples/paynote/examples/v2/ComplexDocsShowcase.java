package blue.language.samples.paynote.examples.v2;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.MyOs;
import blue.language.samples.paynote.dsl.MyOsTimeline;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.examples.CardTransactionPayNoteBootstrapExample;
import blue.language.samples.paynote.examples.ShipmentCapturePayNoteTemplates;
import blue.language.samples.paynote.examples.CandidateCvBootstrapExample;
import blue.language.samples.paynote.examples.RecruitmentClassifierBootstrapExample;
import blue.language.samples.paynote.sdk.v2.PayNoteOverlay;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ComplexDocsShowcase {

    private ComplexDocsShowcase() {
    }

    public static Map<String, Node> buildAll(String timestamp) {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();

        docs.put("candidateCvBootstrap",
                CandidateCvBootstrapExample.build(timestamp, "SESSION_RECRUITMENT_MAIN", "ACC_RECRUITER"));

        docs.put("recruitmentClassifierBootstrap",
                RecruitmentClassifierBootstrapExample.build(
                        timestamp, "SESSION_RECRUITMENT_MAIN", "SESSION_LLM_PROVIDER", "ACC_RECRUITER"));

        docs.put("timedCaptureVoucher", timedCaptureVoucher());
        docs.put("shippingAndSportsBarrier", shippingAndSportsBarrier());
        docs.put("marketplaceSplit", marketplaceSplit());
        docs.put("agentBudgetController", agentBudgetController());
        docs.put("milestoneContractorFlow", milestoneContractorFlow());
        docs.put("reverseVoucherCreditLine", reverseVoucherCreditLine());
        docs.put("fraudHoldAndRelease", fraudHoldAndRelease());
        docs.put("dualApprovalEscrow", dualApprovalEscrow());
        docs.put("escrowCancellationAndExpiry", escrowCancellationAndExpiry());
        docs.put("partialShipmentCapture", partialShipmentCapture());
        docs.put("settlementAdjustmentFlow", settlementAdjustmentFlow());
        docs.put("disputeAdjudicationFlow", disputeAdjudicationFlow());
        docs.put("fxQuoteLockFlow", fxQuoteLockFlow());
        docs.put("cardRailVariant", cardRailVariant(timestamp));
        docs.put("bankTransferRailVariant", bankTransferRailVariant(timestamp));

        return docs;
    }

    private static Node timedCaptureVoucher() {
        Node payNote = PayNoteOverlay.payNote("Timed Capture Voucher", 180, "EUR")
                .abstractParticipants()
                .reserve(180)
                .captureAfterTimer("/timer/captureAt", 180)
                .once("captureOnlyOnce", PayNoteTypes.FundsReserved.class, "/flags/capturedOnce", steps -> steps
                        .replaceValue("MarkCapturedByOnce", "/flags/capturedByOnceMacro", true))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node shippingAndSportsBarrier() {
        Node payNote = PayNoteOverlay.payNote("Shipping + Sports Barrier Capture", 520, "EUR")
                .abstractParticipants()
                .reserve(520)
                .barrier("waitForShipmentAndOutcome",
                        CommonTypes.NamedEvent.class,
                        "/barriers/shipmentAndOutcome",
                        "event.name",
                        PayNoteOverlay.defaultBarrierSignals("shipment-delivered", "match-won"),
                        steps -> steps
                                .emitType("CaptureAfterBarrier", PayNoteTypes.CaptureFundsRequested.class,
                                        payload -> payload.put("amount", 520))
                                .replaceValue("SetBarrierCaptured", "/status", "captured-after-barrier"))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node marketplaceSplit() {
        Node payNote = PayNoteOverlay.payNote("Marketplace Split", 1200, "EUR")
                .abstractParticipants()
                .reserve(1200)
                .issueChildOnEvent("issueChildrenOnReserve", PayNoteTypes.FundsReserved.class, "/children")
                .withOperation("allocateSplit",
                        "payerChannel",
                        "Allocate seller/platform split and mark prepared child envelopes",
                        changeset -> changeset
                                .replaceValue("/children/seller/amount", 1000)
                                .replaceValue("/children/platform/amount", 200))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node agentBudgetController() {
        Node payNote = PayNoteOverlay.payNote("Agent Budget Controller", 400, "USD")
                .abstractParticipants()
                .reserve(400)
                .once("issueBudgetChildOnce", ConversationTypes.ChatMessage.class, "/flags/childIssued", steps -> steps
                        .emitType("IssueBudgetChild", PayNoteTypes.IssueChildPayNoteRequested.class,
                                payload -> payload.put("childPayNote", new Node().value("/children/agent-task"))))
                .onEvent("cancelBudgetOnCommand", ConversationTypes.ChatMessage.class, steps -> steps
                        .emitType("CancelBudgetChild", PayNoteTypes.PayNoteCancellationRequested.class,
                                payload -> payload.put("childPayNote", new Node().value("/children/agent-task")))
                        .replaceValue("MarkChildCancelled", "/children/agent-task/status", "cancelled"))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node milestoneContractorFlow() {
        Node payNote = PayNoteOverlay.payNote("Milestone Contractor", 900, "EUR")
                .abstractParticipants()
                .reserve(900)
                .onChange("captureWhenMilestoneApproved", "/milestones/1/approved", steps -> steps
                        .emitType("CaptureMilestoneOne", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 300))
                        .replaceValue("MarkMilestoneOneCaptured", "/milestones/1/captured", true))
                .onChange("captureWhenMilestoneTwoApproved", "/milestones/2/approved", steps -> steps
                        .emitType("CaptureMilestoneTwo", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 600))
                        .replaceValue("MarkMilestoneTwoCaptured", "/milestones/2/captured", true))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node reverseVoucherCreditLine() {
        Node payNote = PayNoteOverlay.payNote("Reverse Voucher Credit Line", 250, "EUR")
                .abstractParticipants()
                .reserveAndCaptureImmediately(250)
                .withOperation("configureCreditLine",
                        "payerChannel",
                        "Attach merchant railReference and monitoring merchant metadata",
                        changeset -> changeset
                                .replaceValue("/merchant/railReference", "RAIL-MERCHANT-XYZ")
                                .replaceValue("/merchant/monitoringMerchantId", "MERCHANT-777"))
                .onEvent("triggerCaptureFromMerchantObservation", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CaptureAfterMerchantObservation", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 250)))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node fraudHoldAndRelease() {
        Node payNote = PayNoteOverlay.payNote("Fraud Hold + Release", 310, "USD")
                .abstractParticipants()
                .reserve(310)
                .onEvent("freezeOnFraudSignal", CommonTypes.NamedEvent.class, steps -> steps
                        .replaceValue("SetStatusOnHold", "/status", "on-hold")
                        .emitAdHocEvent("NotifyFraudHold", "fraud-hold-notification", payload -> payload
                                .put("severity", "high")
                                .put("reason", "automated-risk-score")))
                .onEvent("releaseOnManualReview", ConversationTypes.ChatMessage.class, steps -> steps
                        .replaceValue("SetStatusReleased", "/status", "released")
                        .emitType("CaptureAfterRelease", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 310)))
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node dualApprovalEscrow() {
        Node payNote = PayNoteOverlay.payNote("Dual Approval Escrow", 760, "EUR")
                .abstractParticipants()
                .reserve(760)
                .barrier("requireBuyerAndArbitratorApproval",
                        CommonTypes.NamedEvent.class,
                        "/barriers/dualApproval",
                        "event.name",
                        PayNoteOverlay.defaultBarrierSignals("buyer-approved", "arbitrator-approved"),
                        steps -> steps
                                .emitType("CaptureEscrowAfterDualApproval", PayNoteTypes.CaptureFundsRequested.class,
                                        payload -> payload.put("amount", 760))
                                .replaceValue("SetStatusEscrowReleased", "/status", "released"))
                .refundFullOperation()
                .build();
        return bindDefaultParticipants(payNote);
    }

    private static Node escrowCancellationAndExpiry() {
        PayNoteOverlay payNote = PayNoteOverlay.payNote("Escrow Cancellation + Expiry", 430, "EUR")
                .abstractParticipants()
                .reserve(430)
                .withOperation("requestCancellation",
                        "payerChannel",
                        "Payer requests cancellation for protection flow",
                        changeset -> changeset.replaceValue("/status", "cancellation-requested"))
                .onEvent("releaseOnCancellationApproval", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("ReleaseReservationAfterCancellation",
                                PayNoteTypes.ReservationReleaseRequested.class,
                                payload -> payload.put("amount", 430))
                        .replaceValue("MarkCancelled", "/status", "cancelled"))
                .onChange("expiryTimeout", "/timeouts/expiryAt", steps -> steps
                        .emitType("ReleaseOnExpiry", PayNoteTypes.ReservationReleaseRequested.class,
                                payload -> payload.put("amount", 430))
                        .replaceValue("MarkExpired", "/status", "expired"));
        return bindDefaultParticipants(payNote.build());
    }

    private static Node partialShipmentCapture() {
        PayNoteOverlay payNote = PayNoteOverlay.payNote("Partial Shipment Capture", 1200, "EUR")
                .abstractParticipants()
                .reserve(1200)
                .onEvent("capturePartialOne", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CapturePartOne", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 500))
                        .replaceValue("TrackPartialOne", "/partials/1/captured", true))
                .onEvent("capturePartialTwo", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CapturePartTwo", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 700))
                        .replaceValue("TrackPartialTwo", "/partials/2/captured", true));
        return bindDefaultParticipants(payNote.build());
    }

    private static Node settlementAdjustmentFlow() {
        PayNoteOverlay payNote = PayNoteOverlay.payNote("Settlement Adjustment", 990, "EUR")
                .abstractParticipants()
                .reserve(990)
                .withOperation("specifySettlementAmount",
                        "payeeChannel",
                        "Payee specifies final settlement amount before capture",
                        changeset -> changeset.replaceExpression("/settlement/finalAmount", "event.message.request.amount"))
                .onEvent("captureAfterSettlementAccepted", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CaptureFinalSettlement", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.putExpression("amount", "document('/settlement/finalAmount')"))
                        .replaceValue("MarkSettlementCaptured", "/status", "captured"));
        return bindDefaultParticipants(payNote.build());
    }

    private static Node disputeAdjudicationFlow() {
        PayNoteOverlay payNote = PayNoteOverlay.payNote("Dispute Adjudication", 840, "EUR")
                .abstractParticipants()
                .reserve(840)
                .onEvent("markDisputed", CommonTypes.NamedEvent.class, steps -> steps
                        .replaceValue("SetDisputed", "/status", "disputed"))
                .onEvent("captureIfDisputeRejected", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CaptureAfterDisputeReject", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 840))
                        .replaceValue("SetCaptured", "/status", "captured"))
                .onEvent("releaseIfDisputeApproved", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("ReleaseAfterDisputeApprove", PayNoteTypes.ReservationReleaseRequested.class,
                                payload -> payload.put("amount", 840))
                        .replaceValue("SetRefunded", "/status", "refunded"));
        return bindDefaultParticipants(payNote.build());
    }

    private static Node fxQuoteLockFlow() {
        PayNoteOverlay payNote = PayNoteOverlay.payNote("FX Quote Lock Escrow", 500, "EUR")
                .abstractParticipants()
                .reserve(500)
                .withOperation("lockFxQuote",
                        "guarantorChannel",
                        "Guarantor locks FX quote before reserve execution",
                        changeset -> changeset
                                .replaceValue("/funding/sourceCurrency", "CHF")
                                .replaceValue("/funding/fxQuoteId", "FX-QUOTE-2026-001"))
                .onEvent("captureWhenFxAndShipmentReady", CommonTypes.NamedEvent.class, steps -> steps
                        .emitType("CaptureAfterFxLock", PayNoteTypes.CaptureFundsRequested.class,
                                payload -> payload.put("amount", 500)));
        return bindDefaultParticipants(payNote.build());
    }

    private static Node cardRailVariant(String timestamp) {
        return CardTransactionPayNoteBootstrapExample.build(
                timestamp,
                "payer@card-rail",
                "payee@merchant-card",
                "guarantor@bank-card",
                "shipment@dhl-card");
    }

    private static Node bankTransferRailVariant(String timestamp) {
        Node template = ShipmentCapturePayNoteTemplates.captureOnShipmentTemplate(timestamp);
        Node specialized = ShipmentCapturePayNoteTemplates.eur200FromChfWithDhl(template, "shipment@bank-transfer");
        return ShipmentCapturePayNoteTemplates.instantiateForAliceBob(
                specialized,
                "payer@bank-transfer",
                "payee@merchant-bank",
                "guarantor@bank-transfer");
    }

    private static Node bindDefaultParticipants(Node document) {
        return MyOs.bootstrap(document)
                .bindTimeline("payerChannel", MyOsTimeline.email("payer@demo.com"))
                .bindTimeline("payeeChannel", MyOsTimeline.accountId("PAYEE-001"))
                .bindTimeline("guarantorChannel", MyOsTimeline.accountId("0"))
                .bindTimeline("triggeredEventChannel", MyOsTimeline.accountId("0"))
                .bindTimeline("myOsAdminChannel", MyOsTimeline.accountId("0"))
                .build();
    }
}
