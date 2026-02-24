package blue.language.samples.paynote.examples.paynote;

import blue.language.model.Node;
import blue.language.samples.paynote.examples.voucher.ArmchairProtectionWithVoucherPayNote;
import blue.language.samples.paynote.examples.voucher.BalancedBowlVoucherPayNote;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.CookbookEvents;
import blue.language.samples.paynote.types.domain.VoucherEvents;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PayNoteCookbookExamplesV2 {

    private PayNoteCookbookExamplesV2() {
    }

    public static Map<String, Node> tier1Tiny() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.put("ticket01_satisfactionUnlockCapture", ticket01SatisfactionUnlockCapture());
        docs.put("ticket02_satisfactionVoucherCreditLine", ticket02SatisfactionVoucherCreditLine());
        docs.put("ticket03_voucherCaptureUpToBudget", ticket03VoucherCaptureUpToBudget());
        docs.put("ticket04_deliveryEscrowShipperCancel", ticket04DeliveryEscrowShipperCancel());
        docs.put("ticket05_deliveryDisputeWindowRefund", ticket05DeliveryDisputeWindowRefund());
        docs.put("ticket06_milestoneContractorFourStage", ticket06MilestoneContractorFourStage());
        docs.put("ticket07_marketplaceSplit1585", ticket07MarketplaceSplit1585());
        docs.put("ticket08_subscriptionMonthlyStopOnCancel", ticket08SubscriptionMonthlyStopOnCancel());
        docs.put("ticket09_agentBudgetRemaining", ticket09AgentBudgetRemaining());
        docs.put("ticket10_approvalRequiredThenUnlock", ticket10ApprovalRequiredThenUnlock());
        return docs;
    }

    public static Map<String, Node> tier2Medium() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.put("ticket11_travelBookingChain", ticket11TravelBookingChain());
        docs.put("ticket12_insuranceStagedPayout", ticket12InsuranceStagedPayout());
        docs.put("ticket13_factoringAdvanceSettle", ticket13FactoringAdvanceSettle());
        docs.put("ticket14_bankTransferEscrowMultiApproval", ticket14BankTransferEscrowMultiApproval());
        docs.put("ticket15_cryptoEscrowOnChainRelease", ticket15CryptoEscrowOnChainRelease());
        docs.put("ticket16_lateDeliveryPenaltyPartialRefund", ticket16LateDeliveryPenaltyPartialRefund());
        docs.put("ticket17_returnWindowRefund", ticket17ReturnWindowRefund());
        docs.put("ticket18_tipAdjustmentPartialCapture", ticket18TipAdjustmentPartialCapture());
        docs.put("ticket19_kycGating", ticket19KycGating());
        docs.put("ticket20_fraudHoldManualReview", ticket20FraudHoldManualReview());
        return docs;
    }

    public static Map<String, Node> tier3JsHeavy() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.put("ticket21_donationRoundUpJs", ticket21DonationRoundUpJs());
        docs.put("ticket22_giftCardFundingAndRedemptionJs", ticket22GiftCardFundingAndRedemptionJs());
        docs.put("ticket23_corporateSpendAllowlistJs", ticket23CorporateSpendAllowlistJs());
        docs.put("ticket24_merchantFinancingInstallmentsJs", ticket24MerchantFinancingInstallmentsJs());
        docs.put("ticket25_voucherMonitoringBudgetJs", ticket25VoucherMonitoringBudgetJs());
        return docs;
    }

    public static Map<String, Node> allTickets() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.putAll(tier1Tiny());
        docs.putAll(tier2Medium());
        docs.putAll(tier3JsHeavy());
        return docs;
    }

    public static Node ticket01SatisfactionUnlockCapture() {
        return PayNotes.payNote("Ticket 01 — Satisfaction Unlock Capture")
                .captureLockedUntilOperation("confirmSatisfaction",
                        "payerChannel",
                        "Confirm satisfaction before capture.",
                        VoucherEvents.SatisfactionConfirmed.class)
                .buildDocument();
    }

    public static Node ticket02SatisfactionVoucherCreditLine() {
        return ArmchairProtectionWithVoucherPayNote.templateDoc();
    }

    public static Node ticket03VoucherCaptureUpToBudget() {
        return BalancedBowlVoucherPayNote.templateDoc();
    }

    public static Node ticket04DeliveryEscrowShipperCancel() {
        return PayNoteCookbookExamples.shipmentEscrowCancelable();
    }

    public static Node ticket05DeliveryDisputeWindowRefund() {
        return PayNotes.payNote("Ticket 05 — Delivery + Dispute Refund")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("650.00")
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(CookbookEvents.DeliveryConfirmed.class)
                    .refundOnOperation("openDispute", op -> op
                            .channel("payerChannel")
                            .description("Open dispute within refund window.")
                            .noRequest())
                    .done()
                .buildDocument();
    }

    public static Node ticket06MilestoneContractorFourStage() {
        return PayNoteCookbookExamples.milestoneContractorFourStage();
    }

    public static Node ticket07MarketplaceSplit1585() {
        return PayNoteCookbookExamples.marketplaceSplit8515();
    }

    public static Node ticket08SubscriptionMonthlyStopOnCancel() {
        return PayNotes.payNote("Ticket 08 — Subscription Monthly Stop On Cancel")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("29.00")
                .reserveOnInit()
                .capture().requestOnEvent(CookbookEvents.EventDateReached.class).done()
                .requestCancellationOperation("payerChannel")
                .buildDocument();
    }

    public static Node ticket09AgentBudgetRemaining() {
        return PayNoteCookbookExamples.agentBudgetWithChildPurchases();
    }

    public static Node ticket10ApprovalRequiredThenUnlock() {
        return PayNotes.payNote("Ticket 10 — Approval Required Then Unlock")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("approveRelease", op -> op
                            .channel("guarantorChannel")
                            .description("Guarantor approves release.")
                            .noRequest())
                    .done()
                .buildDocument();
    }

    public static Node ticket11TravelBookingChain() {
        return PayNoteCookbookExamples.agentTripFlightCancellation();
    }

    public static Node ticket12InsuranceStagedPayout() {
        return PayNoteCookbookExamples.insurancePayoutStaged();
    }

    public static Node ticket13FactoringAdvanceSettle() {
        return PayNoteCookbookExamples.invoiceFactoring();
    }

    public static Node ticket14BankTransferEscrowMultiApproval() {
        return PayNotes.payNote("Ticket 14 — Bank Transfer Escrow")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("12000.00")
                .participant("inspectorChannel", "Property inspector")
                .participant("titleCompanyChannel", "Title company")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("inspectorApproval", op -> op
                            .channel("inspectorChannel")
                            .description("Inspector approval")
                            .noRequest())
                    .unlockOnOperation("titleApproval", op -> op
                            .channel("titleCompanyChannel")
                            .description("Title approval")
                            .noRequest())
                    .requestOnOperation("buyerFinalApproval", op -> op
                            .channel("payerChannel")
                            .description("Buyer final approval")
                            .noRequest())
                    .done()
                .buildDocument();
    }

    public static Node ticket15CryptoEscrowOnChainRelease() {
        return PayNotes.payNote("Ticket 15 — Crypto Escrow On-chain Release")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("750.00")
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(CookbookEvents.EventDateReached.class)
                    .done()
                .buildDocument();
    }

    public static Node ticket16LateDeliveryPenaltyPartialRefund() {
        return PayNotes.payNote("Ticket 16 — Late Delivery Penalty")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("420.00")
                .capture()
                    .requestPartialOnEvent(CookbookEvents.DeliveryConfirmed.class, "42000")
                    .refundPartialOnEvent(CookbookEvents.DisputeOpened.class, "5000")
                    .done()
                .buildDocument();
    }

    public static Node ticket17ReturnWindowRefund() {
        return PayNotes.payNote("Ticket 17 — Return Window Refund")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("215.00")
                .capture()
                    .requestOnOperation("captureOnDelivery", op -> op
                            .channel("shipmentCompanyChannel")
                            .description("Capture after delivery.")
                            .noRequest())
                    .refundOnOperation("requestReturnRefund", op -> op
                            .channel("payerChannel")
                            .description("Request refund in return window.")
                            .noRequest())
                    .done()
                .participant("shipmentCompanyChannel", "Shipment company")
                .buildDocument();
    }

    public static Node ticket18TipAdjustmentPartialCapture() {
        return PayNotes.payNote("Ticket 18 — Tip Adjustment")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("120.00")
                .capture()
                    .requestPartialOnOperation("approveBaseFare", "10000", op -> op
                            .channel("payerChannel")
                            .description("Approve base fare"))
                    .requestPartialOnOperation("approveTip", "2000", op -> op
                            .channel("payerChannel")
                            .description("Approve tip"))
                    .done()
                .buildDocument();
    }

    public static Node ticket19KycGating() {
        return PayNoteCookbookExamples.internalLedgerKycFirst();
    }

    public static Node ticket20FraudHoldManualReview() {
        return PayNotes.payNote("Ticket 20 — Fraud Hold Manual Review")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("340.00")
                .capture()
                    .lockOnInit()
                    .lockOnEvent(CookbookEvents.DisputeOpened.class)
                    .unlockOnOperation("manualReviewClear", op -> op
                            .channel("guarantorChannel")
                            .description("Manual review clears hold.")
                            .noRequest())
                    .requestOnOperation("captureAfterClearance", op -> op
                            .channel("guarantorChannel")
                            .description("Capture after manual review.")
                            .noRequest())
                    .done()
                .buildDocument();
    }

    public static Node ticket21DonationRoundUpJs() {
        return PayNotes.payNote("Ticket 21 — Donation Round-up")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("35.40")
                .onFundsCaptured("roundUpDonation", steps -> steps
                        .jsTemplate("RoundUpDonation", """
                                const total = Number(document('/amount/total') ?? 0);
                                const cents = total % 100;
                                const donation = cents === 0 ? 0 : 100 - cents;
                                if (donation <= 0) return { events: [] };
                                return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}', amount: donation }] };
                                """))
                .buildDocument();
    }

    public static Node ticket22GiftCardFundingAndRedemptionJs() {
        return PayNotes.payNote("Ticket 22 — Gift Card Funding + Redemption")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("100.00")
                .onEvent("redeemGiftCard", CookbookEvents.UsageReported.class, steps -> steps
                        .jsTemplate("RedeemGiftCardUsage", """
                                const spent = Number(event.message.units ?? 0);
                                const total = Number(document('/amount/total') ?? 0);
                                const captured = Number(document('/amount/captured') ?? 0);
                                const remaining = Math.max(total - captured, 0);
                                const charge = Math.min(spent, remaining);
                                return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}', amount: charge }] };
                                """))
                .buildDocument();
    }

    public static Node ticket23CorporateSpendAllowlistJs() {
        return PayNotes.payNote("Ticket 23 — Corporate Spend Allowlist")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("900.00")
                .onEvent("onCorporateSpendReported", CookbookEvents.UsageReported.class, steps -> steps
                        .jsTemplate("RequireAllowlistAndReceipt", """
                                const merchant = event.message.merchantId;
                                const hasReceipt = event.message.hasReceipt === true;
                                const allowlist = ['airline_a', 'hotel_b', 'uber'];
                                if (!allowlist.includes(merchant) || !hasReceipt) {
                                  return { events: [] };
                                }
                                const amount = Number(event.message.amount ?? 0);
                                return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}', amount }] };
                                """))
                .buildDocument();
    }

    public static Node ticket24MerchantFinancingInstallmentsJs() {
        return PayNotes.payNote("Ticket 24 — Merchant Financing Installments")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("1500.00")
                .onEvent("onInstallmentDue", CookbookEvents.InstallmentDue.class, steps -> steps
                        .jsTemplate("InstallmentCaptureOrStop", """
                                const status = String(document('/status') ?? 'active');
                                if (status === 'defaulted') return { events: [] };
                                const due = Number(event.message.amount ?? 0);
                                const total = Number(document('/amount/total') ?? 0);
                                const captured = Number(document('/amount/captured') ?? 0);
                                const remaining = Math.max(total - captured, 0);
                                const amount = Math.min(due, remaining);
                                return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}', amount }] };
                                """))
                .buildDocument();
    }

    public static Node ticket25VoucherMonitoringBudgetJs() {
        return PayNotes.payNote("Ticket 25 — Voucher Monitoring Budget")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("100.00")
                .onEvent("monitorVoucherUsage", VoucherEvents.RestaurantTransactionReported.class, steps -> steps
                        .jsTemplate("CaptureUpToVoucherBudget", """
                                const spent = Number(event.message.amount ?? 0);
                                const total = Number(document('/amount/total') ?? 0);
                                const captured = Number(document('/amount/captured') ?? 0);
                                const remaining = Math.max(total - captured, 0);
                                const amount = Math.min(spent, remaining);
                                if (amount <= 0) return { events: [] };
                                return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}', amount }] };
                                """))
                .buildDocument();
    }
}
