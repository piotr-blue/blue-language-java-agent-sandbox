package blue.language.samples.paynote.examples.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.examples.ShipmentCapturePayNoteTemplates;
import blue.language.samples.paynote.sdk.vnext.PayNotes;
import blue.language.samples.paynote.types.domain.PayNoteDemoEvents;
import blue.language.samples.paynote.types.domain.ShippingEvents;

public final class VNextPayNoteExamples {

    private VNextPayNoteExamples() {
    }

    public static Node iphoneShipmentEscrow() {
        return PayNotes.cardTransaction("iPhone Purchase")
                .currency("USD")
                .amountTotal(80000)
                .participants(p -> p.payer().payee().guarantor().shipper("shipmentCompanyChannel"))
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
                .directChangeWithAllowList("directChange",
                        "payeeChannel",
                        "Allow note/tracking updates only.",
                        "/note", "/shipping/trackingNumber")
                .initialStateDescription(
                        "This is a protected payment of **$800.00**.",
                        "Funds remain protected until shipment confirmation is emitted by the shipment company.")
                .bindRoleEmail("payer", "alice@gmail.com")
                .bindRoleAccount("payee", "acc_bob_1234")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .bindRoleAccount("shipmentCompany", "acc_dhl_001")
                .build();
    }

    public static TemplateChain shipmentEscrowTemplateChain(String timestamp) {
        DocTemplate base = ShipmentCapturePayNoteTemplates.shipmentEscrowTemplate(timestamp);
        DocTemplate specialized = ShipmentCapturePayNoteTemplates.eur200FromChfWithDhl(base, "acc_dhl_001");
        Node instance = ShipmentCapturePayNoteTemplates.instantiateForAliceBob(
                specialized,
                "acc_alice_001",
                "acc_bob_1234",
                "acc_bank_1");
        return new TemplateChain(base.build(), specialized.build(), instance);
    }

    public static Node subscriptionPayNote() {
        return PayNotes.payNote("Subscription Parent PayNote")
                .currency("EUR")
                .amountTotal(3000)
                .participants(p -> p.payer().payee().guarantor())
                .reserveOnInit()
                .issueChildPayNoteOnEvent("issueMonthlyChild",
                        PayNoteDemoEvents.SubscriptionCycleStarted.class,
                        "document('/children/monthly')")
                .once("issueMonthlyChildOnce",
                        PayNoteDemoEvents.SubscriptionCycleStarted.class,
                        "/flags/monthlyIssued",
                        steps -> steps.emitType("CaptureHookAudit",
                                PayNoteDemoEvents.CaptureHookRan.class,
                                payload -> payload
                                        .putExpression("amount", "document('/amount/total')")
                                        .put("note", "monthly-child-issued")))
                .bindRoleAccount("payer", "acc_subscriber")
                .bindRoleAccount("payee", "acc_saas_vendor")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .build();
    }

    public static Node marketplaceSplitPayNote() {
        return PayNotes.payNote("Marketplace Split PayNote")
                .currency("EUR")
                .amountTotal(20000)
                .participants(p -> p.payer().payee().guarantor())
                .reserveOnInit()
                .onFundsReserved("prepareSplitChildren", steps -> steps
                        .replaceValue("SetSellerAmount", "/children/seller/amount", 18000)
                        .replaceValue("SetPlatformAmount", "/children/platform/amount", 2000))
                .onFundsReserved("issueSplitChildren", steps -> steps
                        .emitType("IssueSellerChild", PayNoteDemoEvents.MarketplaceSplitRequested.class,
                                payload -> payload.put("orderId", "ORDER-001")))
                .once("configureSplitOnce",
                        PayNoteDemoEvents.MarketplaceSplitRequested.class,
                        "/flags/splitConfigured",
                        steps -> steps.replaceValue("MarkSplitConfigured", "/flags/splitConfiguredAtLeastOnce", true))
                .bindRoleAccount("payer", "acc_customer_1")
                .bindRoleAccount("payee", "acc_marketplace")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .build();
    }

    public static Node agentBudgetPayNote() {
        return PayNotes.payNote("Agent Budget PayNote")
                .currency("USD")
                .amountTotal(15000)
                .participants(p -> p.payer().payee().guarantor())
                .reserveOnInit()
                .issueChildPayNoteOnEvent("issueAgentPurchaseChild",
                        PayNoteDemoEvents.AgentPurchaseApproved.class,
                        "document('/children/agentPurchase')")
                .once("approvePurchaseOnce",
                        PayNoteDemoEvents.AgentPurchaseApproved.class,
                        "/flags/purchaseApprovedOnce",
                        steps -> steps.replaceValue("MarkPurchaseApprovalProcessed", "/flags/purchaseApprovalProcessed", true))
                .requestCancellationOperation("payerChannel")
                .bindRoleAccount("payer", "acc_team_budget_owner")
                .bindRoleAccount("payee", "acc_agent_vendor")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .build();
    }

    public static Node milestoneContractorPayNote() {
        return PayNotes.payNote("Milestone Contractor PayNote")
                .currency("EUR")
                .amountTotal(120000)
                .participants(p -> p.payer().payee().guarantor())
                .reserveOnInit()
                .captureOnEvent(PayNoteDemoEvents.MilestoneApproved.class, "captureOnMilestone")
                .onCaptureRequested("markMilestoneCaptureRequest", steps -> steps
                        .replaceExpression("SetCaptureAmount", "/milestones/currentCaptureAmount",
                                "document('/amount/total')"))
                .once("recordMilestoneOnce",
                        PayNoteDemoEvents.MilestoneApproved.class,
                        "/flags/milestoneRecorded",
                        steps -> steps.replaceValue("MarkMilestoneRecorded", "/flags/milestoneRecordedValue", true))
                .releaseOperation("releaseUnspentAmount", "guarantorChannel")
                .bindRoleAccount("payer", "acc_enterprise_customer")
                .bindRoleAccount("payee", "acc_contractor")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .build();
    }

    public static Node reversePaymentVoucherPayNote() {
        return PayNotes.payNote("Reverse Payment Voucher PayNote")
                .currency("EUR")
                .amountTotal(5000)
                .participants(p -> p.payer().payee().guarantor())
                .reserveAndCaptureImmediatelyOnInit()
                .captureOnEvent(PayNoteDemoEvents.VoucherTriggered.class, "captureOnVoucherTrigger")
                .operation("monitorExternalTransaction",
                        "payeeChannel",
                        "Stub operation used by monitor to signal voucher eligibility.",
                        steps -> steps.emitType("VoucherTriggered", PayNoteDemoEvents.VoucherTriggered.class,
                                payload -> payload.putExpression("triggerId", "event.message.request.triggerId")))
                .directChangeWithAllowList("directVoucherEdit",
                        "payeeChannel",
                        "Allow voucher metadata notes update.",
                        "/voucher/notes")
                .bindRoleAccount("payer", "acc_merchant")
                .bindRoleAccount("payee", "acc_customer")
                .bindRoleAccount("guarantor", "acc_bank_1")
                .build();
    }

    public static final class TemplateChain {
        public final Node template;
        public final Node specialized;
        public final Node instance;

        private TemplateChain(Node template, Node specialized, Node instance) {
            this.template = template;
            this.specialized = specialized;
            this.instance = instance;
        }
    }
}
