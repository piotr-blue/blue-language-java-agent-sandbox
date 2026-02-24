package blue.language.samples.paynote.examples.paynote;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
