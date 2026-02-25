package blue.language.samples.paynote.examples.paynote;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteCookbookExamplesTest {

    @Test
    void providesAtLeastTwentyFourPayNoteScenarios() {
        Map<String, Node> docs = PayNoteCookbookExamples.all();

        assertTrue(docs.size() >= 24);
        for (Node document : docs.values()) {
            assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"));
        }
    }

    @Test
    void providesTieredCookbookWithTwentyFiveTickets() {
        Map<String, Node> tier1 = PayNoteCookbookExamples.tier1Tiny();
        Map<String, Node> tier2 = PayNoteCookbookExamples.tier2Medium();
        Map<String, Node> tier3 = PayNoteCookbookExamples.tier3JsHeavy();
        Map<String, Node> all = PayNoteCookbookExamples.allTickets();

        assertEquals(10, tier1.size());
        assertEquals(10, tier2.size());
        assertEquals(5, tier3.size());
        assertEquals(25, all.size());
    }

    @Test
    void everyTicketBuildsPayNoteWithName() {
        for (Map.Entry<String, Node> entry : PayNoteCookbookExamples.allTickets().entrySet()) {
            Node document = entry.getValue();
            assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"), entry.getKey());
            assertFalse(document.getAsText("/name/value").trim().isEmpty(), entry.getKey());
        }
    }

    @Test
    void shipmentEscrowCancelableSupportsUnlockAndCancellation() {
        Node document = PayNoteCookbookExamples.shipmentEscrowCancelable();

        assertEquals(TypeAliases.CONVERSATION_OPERATION, document.getAsText("/contracts/confirmShipment/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                document.getAsText("/contracts/confirmShipmentImpl/steps/1/event/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION, document.getAsText("/contracts/requestCancellation/type/value"));
    }

    @Test
    void marketplaceSplitBuildsTwoLedgerPaymentRequests() {
        Node document = PayNoteCookbookExamples.marketplaceSplit8515();

        assertEquals(TypeAliases.PAYMENTS_INTERNAL_LEDGER_TRANSFER_REQUESTED,
                document.getAsText("/contracts/fanOutPlatformAndSeller/steps/0/event/type/value"));
        assertEquals(TypeAliases.PAYMENTS_INTERNAL_LEDGER_TRANSFER_REQUESTED,
                document.getAsText("/contracts/fanOutPlatformAndSeller/steps/1/event/type/value"));
    }

    @Test
    void restaurantVoucherWhitelistCapturesOnlyWhitelistedMerchantInJs() {
        Node document = PayNoteCookbookExamples.restaurantVoucherWhitelist();
        String js = document.getAsText("/contracts/reportSpend/steps/0/code/value");

        assertTrue(js.contains("merchant !== 'balanced_bowl_001'"));
        assertTrue(js.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
    }

    @Test
    void supportsCreditLineAchAndCryptoPaymentSubtypes() {
        Node creditLine = PayNoteCookbookExamples.merchantToCustomerCreditLine();
        Node ach = PayNoteCookbookExamples.achRefundFlow();
        Node crypto = PayNoteCookbookExamples.cryptoPayoutFlow();

        assertEquals(TypeAliases.PAYMENTS_CREDIT_LINE_MERCHANT_TO_CARDHOLDER_PAYMENT_REQUESTED,
                creditLine.getAsText("/contracts/issueCreditLinePayment/steps/0/event/type/value"));
        assertEquals(TypeAliases.PAYMENTS_ACH_TRANSFER_REQUESTED,
                ach.getAsText("/contracts/onRefundRequested/steps/0/event/type/value"));
        assertEquals(TypeAliases.PAYMENTS_CRYPTO_TRANSFER_REQUESTED,
                crypto.getAsText("/contracts/payoutCryptoImpl/steps/0/event/type/value"));
    }

    @Test
    void mediumBankTransferEscrowIncludesExpectedMultiApprovalOperations() {
        Node document = PayNoteCookbookExamples.ticket14BankTransferEscrowMultiApproval();

        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/inspectorApproval/type/value"));
        assertEquals("inspectorChannel", document.getAsText("/contracts/inspectorApproval/channel/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/titleApproval/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/buyerFinalApproval/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_FUNDS_REQUESTED,
                document.getAsText("/contracts/buyerFinalApprovalImpl/steps/0/event/type/value"));
    }

    @Test
    void jsHeavyTicketsContainCriticalCaptureLogicFragments() {
        Node donation = PayNoteCookbookExamples.ticket21DonationRoundUpJs();
        Node corporateSpend = PayNoteCookbookExamples.ticket23CorporateSpendAllowlistJs();
        Node financing = PayNoteCookbookExamples.ticket24MerchantFinancingInstallmentsJs();

        String donationJs = donation.getAsText("/contracts/roundUpDonation/steps/0/code/value");
        assertTrue(donationJs.contains("const cents = total % 100;"));
        assertTrue(donationJs.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));

        String spendJs = corporateSpend.getAsText("/contracts/onCorporateSpendReported/steps/0/code/value");
        assertTrue(spendJs.contains("allowlist.includes(merchant)"));
        assertTrue(spendJs.contains("hasReceipt"));

        String financingJs = financing.getAsText("/contracts/onInstallmentDue/steps/0/code/value");
        assertTrue(financingJs.contains("status === 'defaulted'"));
        assertTrue(financingJs.contains("Math.min(due, remaining)"));
    }
}
