package blue.language.samples.paynote.examples.paynote;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.examples.voucher.BalancedBowlVoucherPayNote;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.domain.CookbookEvents;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.samples.paynote.types.payments.PaymentRequests;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

public final class PayNoteCookbookExamples {

    private PayNoteCookbookExamples() {
    }

    public static Map<String, Node> all() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.put("shipmentEscrowSimple", shipmentEscrowSimple());
        docs.put("shipmentEscrowCancelable", shipmentEscrowCancelable());
        docs.put("shipmentEscrowDisputeWindow", shipmentEscrowDisputeWindow());
        docs.put("marketplaceSplit8515", marketplaceSplit8515());
        docs.put("milestoneContractorFourStage", milestoneContractorFourStage());
        docs.put("milestoneDualApprovalBarrier", milestoneDualApprovalBarrier());
        docs.put("subscriptionMonthly", subscriptionMonthly());
        docs.put("subscriptionTrialThenCapture", subscriptionTrialThenCapture());
        docs.put("agentBudgetWithChildPurchases", agentBudgetWithChildPurchases());
        docs.put("agentTripFlightCancellation", agentTripFlightCancellation());
        docs.put("bnplInstallments", bnplInstallments());
        docs.put("insurancePayoutStaged", insurancePayoutStaged());
        docs.put("invoiceFactoring", invoiceFactoring());
        docs.put("crossBorderFxQuoteAccepted", crossBorderFxQuoteAccepted());
        docs.put("donationMatching", donationMatching());
        docs.put("rentalDepositInspectionRelease", rentalDepositInspectionRelease());
        docs.put("ticketEscrowEventDate", ticketEscrowEventDate());
        docs.put("saasUsageCap", saasUsageCap());
        docs.put("restaurantVoucher", restaurantVoucher());
        docs.put("restaurantVoucherWhitelist", restaurantVoucherWhitelist());
        docs.put("merchantToCustomerCreditLine", merchantToCustomerCreditLine());
        docs.put("achRefundFlow", achRefundFlow());
        docs.put("cryptoPayoutFlow", cryptoPayoutFlow());
        docs.put("internalLedgerKycFirst", internalLedgerKycFirst());
        return docs;
    }

    public static Node shipmentEscrowSimple() {
        return PayNotes.payNote("Cookbook Shipment Escrow")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(120000)
                .captureLockedUntilOperation("confirmDelivery",
                        "guarantorChannel",
                        "Guarantor confirms delivery",
                        CookbookEvents.DeliveryConfirmed.class)
                .buildDocument();
    }

    public static Node shipmentEscrowCancelable() {
        return PayNotes.payNote("Cookbook Shipment Escrow Cancelable")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(90000)
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("confirmShipment", op -> op
                            .channel("shipmentCompanyChannel")
                            .description("Shipment company confirms shipment.")
                            .steps(steps -> steps.emitType("ShipmentConfirmed",
                                    ShippingEvents.ShipmentConfirmed.class,
                                    payload -> payload.put("source", "shipmentCompanyChannel"))))
                    .done()
                .participant("shipmentCompanyChannel", "Shipment company")
                .requestCancellationOperation("payerChannel")
                .buildDocument();
    }

    public static Node shipmentEscrowDisputeWindow() {
        return PayNotes.payNote("Cookbook Shipment Escrow Dispute Window")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("499.00")
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(CookbookEvents.DeliveryConfirmed.class)
                    .done()
                .onEvent("onDisputeOpened", CookbookEvents.DisputeOpened.class, steps -> steps.capture().lock())
                .buildDocument();
    }

    public static Node marketplaceSplit8515() {
        return PayNotes.payNote("Cookbook Marketplace Split 85/15")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("450.00")
                .reserveAndCaptureImmediatelyOnInit()
                .onFundsCaptured("fanOutPlatformAndSeller", steps -> steps
                        .triggerPayment("SellerShare",
                                PaymentRequests.InternalLedgerTransferRequested.class,
                                payload -> payload
                                        .put("processor", "guarantorChannel")
                                        .put("payer", "payerChannel")
                                        .put("payee", "payeeChannel")
                                        .put("currency", "USD")
                                        .put("amount", 38250)
                                        .put("ledgerAccountFrom", "wallet_marketplace")
                                        .put("ledgerAccountTo", "wallet_seller"))
                        .triggerPayment("PlatformFee",
                                PaymentRequests.InternalLedgerTransferRequested.class,
                                payload -> payload
                                        .put("processor", "guarantorChannel")
                                        .put("payer", "payerChannel")
                                        .put("payee", "platformChannel")
                                        .put("currency", "USD")
                                        .put("amount", 6750)
                                        .put("ledgerAccountFrom", "wallet_marketplace")
                                        .put("ledgerAccountTo", "wallet_platform")))
                .participant("platformChannel", "Platform settlement account")
                .buildDocument();
    }

    public static Node milestoneContractorFourStage() {
        return PayNotes.payNote("Cookbook Milestone Contractor")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("1000.00")
                .reserveOnInit()
                .operation("approveMilestoneOne")
                    .channel("guarantorChannel")
                    .description("Approve milestone 1 (25%).")
                    .noRequest()
                    .steps(steps -> steps.capture().requestPartial("25000"))
                    .done()
                .operation("approveMilestoneTwo")
                    .channel("guarantorChannel")
                    .description("Approve milestone 2 (25%).")
                    .noRequest()
                    .steps(steps -> steps.capture().requestPartial("25000"))
                    .done()
                .operation("approveMilestoneThree")
                    .channel("guarantorChannel")
                    .description("Approve milestone 3 (25%).")
                    .noRequest()
                    .steps(steps -> steps.capture().requestPartial("25000"))
                    .done()
                .operation("approveMilestoneFour")
                    .channel("guarantorChannel")
                    .description("Approve milestone 4 (25%).")
                    .noRequest()
                    .steps(steps -> steps.capture().requestPartial("25000"))
                    .done()
                .buildDocument();
    }

    public static Node milestoneDualApprovalBarrier() {
        return PayNotes.payNote("Cookbook Milestone Dual Approval")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("600.00")
                .reserveOnInit()
                .operation("ownerApproval")
                    .channel("payerChannel")
                    .description("Owner approves milestone.")
                    .noRequest()
                    .steps(steps -> steps.emitType("OwnerApproved", CommonTypes.NamedEvent.class,
                            payload -> payload.put("name", "Owner Approved")))
                    .done()
                .operation("inspectorApproval")
                    .channel("inspectorChannel")
                    .description("Inspector approves milestone.")
                    .noRequest()
                    .steps(steps -> steps.emitType("InspectorApproved", CommonTypes.NamedEvent.class,
                            payload -> payload.put("name", "Inspector Approved")))
                    .done()
                .barrier("afterBothApprovals",
                        CommonTypes.NamedEvent.class,
                        "/barrier/approvals",
                        "event.message.name",
                        Arrays.asList("Owner Approved", "Inspector Approved"),
                        steps -> steps.capture().requestNow())
                .participant("inspectorChannel", "Independent inspector")
                .buildDocument();
    }

    public static Node subscriptionMonthly() {
        return PayNotes.payNote("Cookbook Subscription Monthly")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("29.00")
                .reserveOnInit()
                .onEvent("monthlyCycle", CookbookEvents.EventDateReached.class, steps -> steps.capture().requestNow())
                .buildDocument();
    }

    public static Node subscriptionTrialThenCapture() {
        return PayNotes.payNote("Cookbook Subscription Trial")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("14.99")
                .capture()
                    .lockOnInit()
                    .requestOnEvent(CookbookEvents.TrialEnded.class)
                    .done()
                .buildDocument();
    }

    public static Node agentBudgetWithChildPurchases() {
        return PayNotes.payNote("Cookbook Agent Budget")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("250.00")
                .reserveOnInit()
                .issueChildPayNoteOnEvent("issuePurchase", CookbookEvents.UsageReported.class, "payeeChannel")
                .buildDocument();
    }

    public static Node agentTripFlightCancellation() {
        return PayNotes.payNote("Cookbook Agent Trip Flight")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("750.00")
                .reserveOnInit()
                .onEvent("onFlightCancelled", CookbookEvents.DisputeOpened.class, steps -> steps
                        .capture().refundFull()
                        .emitType("FlightCancelled", CommonTypes.NamedEvent.class,
                                payload -> payload.put("name", "Flight Cancelled")))
                .buildDocument();
    }

    public static Node bnplInstallments() {
        return PayNotes.payNote("Cookbook BNPL 4 Installments")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("400.00")
                .reserveOnInit()
                .onEvent("captureOnInstallment", CookbookEvents.InstallmentDue.class,
                        steps -> steps.capture().requestPartial("event.message.amount ?? 10000"))
                .buildDocument();
    }

    public static Node insurancePayoutStaged() {
        return PayNotes.payNote("Cookbook Insurance Payout")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("1200.00")
                .reserveOnInit()
                .operation("requestEmergencyPayout")
                    .channel("payeeChannel")
                    .description("Emergency payout request")
                    .steps(steps -> steps.capture().requestPartial("30000"))
                    .done()
                .operation("requestFinalPayout")
                    .channel("guarantorChannel")
                    .description("Final payout approval")
                    .steps(steps -> steps.capture().requestPartial("90000"))
                    .done()
                .buildDocument();
    }

    public static Node invoiceFactoring() {
        return PayNotes.payNote("Cookbook Invoice Factoring")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("850.00")
                .reserveOnInit()
                .operation("payAdvance")
                    .channel("guarantorChannel")
                    .description("Pay factoring advance")
                    .steps(steps -> steps.capture().requestPartial("68000"))
                    .done()
                .operation("settleInvoice")
                    .channel("payerChannel")
                    .description("Settle remaining amount after collection")
                    .steps(steps -> steps.capture().requestPartial("17000"))
                    .done()
                .buildDocument();
    }

    public static Node crossBorderFxQuoteAccepted() {
        return PayNotes.payNote("Cookbook Cross Border FX")
                .currency(IsoCurrency.EUR)
                .amountTotalMajor("300.00")
                .reserveOnInit()
                .onEvent("onFxAccepted", CookbookEvents.FxQuoteAccepted.class, steps -> steps.capture().requestNow())
                .buildDocument();
    }

    public static Node donationMatching() {
        return PayNotes.payNote("Cookbook Donation Matching")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("100.00")
                .reserveAndCaptureImmediatelyOnInit()
                .onFundsCaptured("triggerMatchTransfer", steps -> steps.triggerPayment(
                        "IssueMatchingTransfer",
                        PaymentRequests.InternalLedgerTransferRequested.class,
                        payload -> payload
                                .put("processor", "guarantorChannel")
                                .put("payer", "matchingFund")
                                .put("payee", "charityWallet")
                                .put("currency", "USD")
                                .put("amount", 10000)
                                .put("ledgerAccountFrom", "fund_matching")
                                .put("ledgerAccountTo", "charity_primary")))
                .buildDocument();
    }

    public static Node rentalDepositInspectionRelease() {
        return PayNotes.payNote("Cookbook Rental Deposit")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("1500.00")
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(CookbookEvents.InspectionPassed.class)
                    .done()
                .buildDocument();
    }

    public static Node ticketEscrowEventDate() {
        return PayNotes.payNote("Cookbook Ticket Escrow")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("79.00")
                .captureLockedUntilEvent(CookbookEvents.EventDateReached.class)
                .buildDocument();
    }

    public static Node saasUsageCap() {
        return PayNotes.payNote("Cookbook SaaS Usage Cap")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("499.00")
                .onEvent("captureUsage", CookbookEvents.UsageReported.class, steps -> steps
                        .jsRaw("CaptureCappedUsage",
                                "const units = Number(event.message.units ?? 0);\n"
                                        + "const cap = Number(document('/amount/total') ?? 0);\n"
                                        + "const captured = Number(document('/amount/captured') ?? 0);\n"
                                        + "const remaining = Math.max(cap - captured, 0);\n"
                                        + "const charge = Math.min(units, remaining);\n"
                                        + "return { events: [{ type: '" + PayNoteAliases.CAPTURE_FUNDS_REQUESTED
                                        + "', amount: charge }] };"))
                .buildDocument();
    }

    public static Node restaurantVoucher() {
        return BalancedBowlVoucherPayNote.templateDoc();
    }

    public static Node restaurantVoucherWhitelist() {
        return PayNotes.payNote("Cookbook Restaurant Voucher Whitelist")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("125.00")
                .onEvent("reportSpend", VoucherEvents.RestaurantTransactionReported.class, steps -> steps
                        .jsRaw("CaptureOnlyWhitelistedMerchant",
                                "const merchant = event.message.merchantId;\n"
                                        + "if (merchant !== 'balanced_bowl_001') {\n"
                                        + "  return { events: [] };\n"
                                        + "}\n"
                                        + "const spent = Number(event.message.amount ?? 0);\n"
                                        + "return { events: [{ type: '" + PayNoteAliases.CAPTURE_FUNDS_REQUESTED
                                        + "', amount: spent }] };"))
                .buildDocument();
    }

    public static Node merchantToCustomerCreditLine() {
        return PayNotes.payNote("Cookbook Merchant To Customer Credit Line")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("100.00")
                .reserveAndCaptureImmediatelyOnInit()
                .onFundsCaptured("issueCreditLinePayment", steps -> steps.triggerPayment(
                        "CreditLinePaymentRequested",
                        PaymentRequests.CreditLineMerchantToCardholderPaymentRequested.class,
                        payload -> payload
                                .put("processor", "guarantorChannel")
                                .put("payer", "merchantAccount")
                                .put("payee", "customerAccount")
                                .put("currency", "USD")
                                .put("amount", 10000)
                                .put("creditLineId", "credit-line-123")))
                .buildDocument();
    }

    public static Node achRefundFlow() {
        return PayNotes.payNote("Cookbook ACH Refund")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("250.00")
                .refundOnOperation("requestRefund", "payeeChannel", "Request ACH refund")
                .onEvent("onRefundRequested", PayNoteTypes.ReservationReleaseRequested.class, steps -> steps.triggerPayment(
                        "IssueAchRefund",
                        PaymentRequests.AchTransferRequested.class,
                        payload -> payload
                                .put("processor", "guarantorChannel")
                                .put("payer", "merchantBank")
                                .put("payee", "customerBank")
                                .put("currency", "USD")
                                .put("amount", 25000)
                                .put("sourceIban", "US001")
                                .put("destinationIban", "US002")))
                .buildDocument();
    }

    public static Node cryptoPayoutFlow() {
        return PayNotes.payNote("Cookbook Crypto Payout")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("75.00")
                .reserveAndCaptureImmediatelyOnInit()
                .operation("payoutCrypto")
                    .channel("payeeChannel")
                    .description("Request crypto payout after capture.")
                    .noRequest()
                    .steps(steps -> steps.triggerPayment("TriggerCryptoPayout",
                            PaymentRequests.CryptoTransferRequested.class,
                            payload -> payload
                                    .put("processor", "guarantorChannel")
                                    .put("payer", "treasury")
                                    .put("payee", "beneficiary")
                                    .put("currency", "USD")
                                    .put("amount", 7500)
                                    .put("asset", "USDC")
                                    .put("walletAddress", "0xabc123")))
                    .done()
                .buildDocument();
    }

    public static Node internalLedgerKycFirst() {
        return PayNotes.payNote("Cookbook Internal Ledger KYC")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("60.00")
                .capture()
                    .lockOnInit()
                    .requestOnEvent(CookbookEvents.KycApproved.class)
                    .done()
                .onFundsCaptured("settleLedgerTransfer", steps -> steps.triggerPayment(
                        "IssueInternalLedgerTransfer",
                        PaymentRequests.InternalLedgerTransferRequested.class,
                        payload -> payload
                                .put("processor", "guarantorChannel")
                                .put("payer", "kyc_pool")
                                .put("payee", "approved_user_wallet")
                                .put("currency", "USD")
                                .put("amount", 6000)
                                .put("ledgerAccountFrom", "pool_account")
                                .put("ledgerAccountTo", "user_wallet")))
                .buildDocument();
    }
}
