package blue.language.samples.paynote.examples.v2;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.MyOs;
import blue.language.samples.paynote.dsl.MyOsTimeline;
import blue.language.samples.paynote.types.common.CommonTypes;
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
